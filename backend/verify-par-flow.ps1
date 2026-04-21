param(
    [string]$BaseUrl = "http://127.0.0.1:30000",
    [string]$RedirectUri = "http://192.168.0.108:5173/callback",
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [string]$ClientId = "spa-par-client",
    [string]$Scope = "openid profile email read write",
    [string]$State = "par-script-state"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)

    [Convert]::ToBase64String($Bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function ConvertTo-FormBody {
    param([hashtable]$Values)

    ($Values.GetEnumerator() | ForEach-Object {
        [uri]::EscapeDataString([string]$_.Key) + "=" + [uri]::EscapeDataString([string]$_.Value)
    }) -join "&"
}

function New-HttpClient {
    param(
        [bool]$UseCookies,
        [System.Net.CookieContainer]$CookieContainer,
        [bool]$AllowAutoRedirect = $false
    )

    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.AllowAutoRedirect = $AllowAutoRedirect
    if ($UseCookies) {
        $handler.CookieContainer = $CookieContainer
        $handler.UseCookies = $true
    }

    New-Object System.Net.Http.HttpClient($handler)
}

function Assert-StatusCode {
    param(
        [string]$Label,
        [int]$Actual,
        [int[]]$Expected
    )

    if ($Expected -notcontains $Actual) {
        throw "$Label failed. Expected status $($Expected -join ', '), actual $Actual"
    }
}

$verifierBytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($verifierBytes)
$codeVerifier = ConvertTo-Base64Url $verifierBytes
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$codeChallenge = ConvertTo-Base64Url ($sha256.ComputeHash([System.Text.Encoding]::ASCII.GetBytes($codeVerifier)))

$cookieContainer = New-Object System.Net.CookieContainer
$sessionClient = New-HttpClient -UseCookies $true -CookieContainer $cookieContainer

$loginJson = @{ username = $Username; password = $Password } | ConvertTo-Json
$loginContent = New-Object System.Net.Http.StringContent($loginJson, [System.Text.Encoding]::UTF8, "application/json")
$loginResponse = $sessionClient.PostAsync("$BaseUrl/api/auth/login", $loginContent).Result
$loginBody = $loginResponse.Content.ReadAsStringAsync().Result
Assert-StatusCode -Label "Login" -Actual ([int]$loginResponse.StatusCode) -Expected @(200)

$parForm = ConvertTo-FormBody @{
    response_type = "code"
    client_id = $ClientId
    redirect_uri = $RedirectUri
    scope = $Scope
    state = $State
    code_challenge = $codeChallenge
    code_challenge_method = "S256"
}
$parContent = New-Object System.Net.Http.StringContent($parForm, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded")
$parResponse = $sessionClient.PostAsync("$BaseUrl/api/auth/par", $parContent).Result
$parBody = $parResponse.Content.ReadAsStringAsync().Result
Assert-StatusCode -Label "PAR relay" -Actual ([int]$parResponse.StatusCode) -Expected @(200, 201)
$parJson = $parBody | ConvertFrom-Json
if (-not $parJson.request_uri) {
    throw "PAR relay failed. Missing request_uri in response: $parBody"
}

$authorizeUrl = "$BaseUrl/oauth2/authorize?client_id=$ClientId&request_uri=$([uri]::EscapeDataString($parJson.request_uri))"
$authorizeResponse = $sessionClient.GetAsync($authorizeUrl).Result
Assert-StatusCode -Label "Authorize redirect" -Actual ([int]$authorizeResponse.StatusCode) -Expected @(302)
if ($null -eq $authorizeResponse.Headers.Location) {
    throw "Authorize redirect failed. Missing Location header"
}

$callbackUri = [System.Uri]$authorizeResponse.Headers.Location.ToString()
$query = [System.Web.HttpUtility]::ParseQueryString($callbackUri.Query)
$authorizationCode = $query["code"]
$returnedState = $query["state"]
if (-not $authorizationCode) {
    throw "Authorize redirect failed. Missing authorization code in callback URI: $callbackUri"
}
if ($returnedState -ne $State) {
    throw "State mismatch. Expected '$State', actual '$returnedState'"
}

$tokenClient = New-HttpClient -UseCookies $false -CookieContainer $null
$tokenForm = ConvertTo-FormBody @{
    grant_type = "authorization_code"
    client_id = $ClientId
    code = $authorizationCode
    redirect_uri = $RedirectUri
    code_verifier = $codeVerifier
}
$tokenContent = New-Object System.Net.Http.StringContent($tokenForm, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded")
$tokenResponse = $tokenClient.PostAsync("$BaseUrl/oauth2/token", $tokenContent).Result
$tokenBody = $tokenResponse.Content.ReadAsStringAsync().Result
Assert-StatusCode -Label "Token exchange" -Actual ([int]$tokenResponse.StatusCode) -Expected @(200)
$tokenJson = $tokenBody | ConvertFrom-Json
if (-not $tokenJson.access_token) {
    throw "Token exchange failed. Missing access_token in response: $tokenBody"
}

$summary = [ordered]@{
    LOGIN_STATUS = [int]$loginResponse.StatusCode
    LOGIN_BODY = $loginBody
    PAR_STATUS = [int]$parResponse.StatusCode
    PAR_BODY = $parBody
    AUTH_STATUS = [int]$authorizeResponse.StatusCode
    AUTH_LOCATION = $callbackUri.ToString()
    AUTH_CODE = $authorizationCode
    AUTH_STATE = $returnedState
    TOKEN_STATUS = [int]$tokenResponse.StatusCode
    TOKEN_SCOPE = $tokenJson.scope
    TOKEN_TYPE = $tokenJson.token_type
    EXPIRES_IN = $tokenJson.expires_in
}

$summary.GetEnumerator() | ForEach-Object {
    Write-Output ("$($_.Key)=$($_.Value)")
}

Write-Output "PAR_FLOW_VERIFIED=true"