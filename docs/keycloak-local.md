# 로컬 Keycloak 실행 및 Realm 초기화

## 1. 목적

이 문서는 HIA User Service의 로컬 Keycloak 환경을 실행하고 `hia` Realm 설정을 자동으로 초기화하는 방법을 설명한다.

현재 로컬 인증 환경은 다음을 제공한다.

- Keycloak 26.8.1
- Keycloak 전용 PostgreSQL 17
- `hia` Realm 자동 생성
- `hia-web` Public OIDC Client
- Authorization Code Flow
- PKCE `S256`
- `hia-api` Audience가 포함된 Access Token
- 사용자, 비밀번호 및 실제 Realm Secret이 제외된 초기 설정

이 구성은 로컬 개발 전용이다. `start-dev` 설정을 운영 환경에서 사용하면 안 된다.

---

## 2. 구성 요소

| 구성 요소 | 역할 |
|---|---|
| `hia-web` | 브라우저에서 Keycloak 로그인을 요청하는 Public OIDC Client |
| `hia` Realm | HIA 사용자의 인증 환경 |
| `hia-api` Client Scope | Access Token에 `hia-api` Audience를 추가 |
| `hia-postgres` | User Service용 PostgreSQL |
| `hia-keycloak-postgres` | Keycloak 전용 PostgreSQL |
| `hia-realm.json` | 새 환경에서 가져올 Realm 초기 설정 |

Keycloak 사용자와 User Service의 회원 도메인 데이터는 서로 다른 데이터베이스에서 관리한다.

Keycloak은 로그인 정보, Credential, Role 및 세션을 관리하고 User Service는 회원 상태와 서비스 도메인 정보를 관리한다.

---

## 3. 사전 요구사항

다음 프로그램이 필요하다.

- Docker Desktop
- Docker Compose
- PowerShell

기본 포트는 다음과 같다.

| 서비스 | 포트 |
|---|---:|
| User Service PostgreSQL | `5432` |
| Keycloak | `8180` |
| 프런트엔드 개발 서버 | `5173` |

---

## 4. 환경변수 설정

저장소 루트에서 `.env.example`을 복사한다.

```powershell
Copy-Item .env.example .env
```

`.env`에 다음 값을 설정한다.

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/hia_user
DB_USERNAME=
DB_PASSWORD=

KEYCLOAK_DB_USERNAME=
KEYCLOAK_DB_PASSWORD=

KEYCLOAK_HTTP_PORT=8180

KEYCLOAK_ADMIN_USERNAME=
KEYCLOAK_ADMIN_PASSWORD=

HIA_KC_HMAC_SECRET=
HIA_KC_AES_SECRET=
```

각 변수의 역할은 다음과 같다.

| 환경변수 | 역할 |
|---|---|
| `DB_USERNAME` | User Service DB 계정 |
| `DB_PASSWORD` | User Service DB 비밀번호 |
| `KEYCLOAK_DB_USERNAME` | Keycloak DB 계정 |
| `KEYCLOAK_DB_PASSWORD` | Keycloak DB 비밀번호 |
| `KEYCLOAK_HTTP_PORT` | 호스트에서 접근할 Keycloak 포트 |
| `KEYCLOAK_ADMIN_USERNAME` | 로컬 Keycloak 관리자 계정 |
| `KEYCLOAK_ADMIN_PASSWORD` | 로컬 Keycloak 관리자 비밀번호 |
| `HIA_KC_HMAC_SECRET` | Realm의 HMAC Key Provider에 사용할 비밀값 |
| `HIA_KC_AES_SECRET` | Realm의 AES Key Provider에 사용할 비밀값 |

### HMAC/AES Secret 생성

다음 PowerShell 명령으로 Base64URL 형식의 Secret을 생성할 수 있다.

```powershell
function New-Base64UrlSecret([int]$byteCount) {
    $bytes = New-Object byte[] $byteCount
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }

    [Convert]::ToBase64String($bytes).
        TrimEnd('=').
        Replace('+', '-').
        Replace('/', '_')
}

"HIA_KC_HMAC_SECRET=$(New-Base64UrlSecret 128)"
"HIA_KC_AES_SECRET=$(New-Base64UrlSecret 32)"
```

출력된 값을 `.env`에 입력한다.

`.env`에는 비밀번호와 Realm Secret이 포함되므로 Git에 커밋하지 않는다. 현재 `.gitignore`는 `.env`를 제외하도록 설정되어 있다.

---

## 5. Compose 설정 검증

컨테이너를 시작하기 전에 Compose 문법과 환경변수 해석을 확인한다.

```powershell
docker compose --env-file .env config --quiet
```

아무 메시지도 출력되지 않고 명령이 종료되면 Compose 구성이 유효한 것이다.

환경변수가 누락된 경우 경고 또는 오류가 표시될 수 있다.

---

## 6. 로컬 환경 시작

전체 서비스를 시작한다.

```powershell
docker compose up -d
```

Keycloak과 Keycloak DB만 시작하려면 다음 명령을 사용한다.

```powershell
docker compose up -d keycloak
```

`depends_on`과 Health Check 설정에 따라 Keycloak PostgreSQL이 정상 상태가 된 후 Keycloak이 시작된다.

컨테이너 상태를 확인한다.

```powershell
docker compose ps
```

Keycloak 로그를 확인한다.

```powershell
docker compose logs --tail 100 keycloak
```

새로운 Keycloak DB에서는 다음과 유사한 로그를 확인할 수 있다.

```text
Importing from directory /opt/keycloak/data/import
Realm 'hia' imported
Import finished successfully
Keycloak 26.8.1 ... started
```

기존 DB에 `hia` Realm이 있으면 다음과 같이 표시된다.

```text
Realm 'hia' already exists. Import skipped
Import finished successfully
Keycloak 26.8.1 ... started
```

기존 Realm을 건너뛰는 것은 정상 동작이다.

---

## 7. 접속 주소

### Keycloak 관리자 콘솔

```text
http://localhost:8180/admin/
```

`.env`의 다음 계정으로 로그인한다.

```text
KEYCLOAK_ADMIN_USERNAME
KEYCLOAK_ADMIN_PASSWORD
```

### HIA Realm OIDC 메타데이터

```text
http://localhost:8180/realms/hia/.well-known/openid-configuration
```

PowerShell에서 확인할 수 있다.

```powershell
$config = Invoke-RestMethod `
    "http://localhost:8180/realms/hia/.well-known/openid-configuration"

[PSCustomObject]@{
    Issuer                = $config.issuer
    AuthorizationEndpoint = $config.authorization_endpoint
    TokenEndpoint         = $config.token_endpoint
    SupportsS256          = @(
        $config.code_challenge_methods_supported
    ) -contains "S256"
}
```

예상 결과는 다음과 같다.

```text
Issuer       : http://localhost:8180/realms/hia
SupportsS256 : True
```

---

## 8. Realm 주요 설정

Realm 이름:

```text
hia
```

로그인 설정:

| 설정 | 값 |
|---|---:|
| User registration | On |
| Login with email | On |
| Verify email | Off |
| Remember me | Off |
| Forgot password | Off |

토큰과 세션 설정:

| 설정 | 값 |
|---|---:|
| Access Token Lifespan | 300초 |
| SSO Session Idle | 1800초 |
| SSO Session Max | 28800초 |

`Verify email`은 현재 로컬 검증 편의를 위해 비활성화되어 있다. 운영 정책을 결정할 때 SMTP 구성과 함께 다시 검토해야 한다.

---

## 9. `hia-web` Client 설정

| 설정 | 값 |
|---|---|
| Client ID | `hia-web` |
| Client type | OpenID Connect |
| Public client | `true` |
| Standard Flow | 활성화 |
| Direct Access Grants | 비활성화 |
| Implicit Flow | 비활성화 |
| PKCE method | `S256` |
| Redirect URI | `http://localhost:5173/` |
| Web Origin | `http://localhost:5173` |

`hia-web`은 브라우저에서 실행되므로 Client Secret을 안전하게 보관할 수 없다. 따라서 Public Client로 구성한다.

Authorization Code 탈취 공격을 방지하기 위해 PKCE `S256`을 필수로 사용한다.

---

## 10. `hia-api` Client Scope

현재 `hia-api`는 Keycloak Client가 아니라 Client Scope이다.

`hia-api` Client Scope에는 다음 Audience Mapper가 등록되어 있다.

| 설정 | 값 |
|---|---|
| Mapper name | `hia-api-audience` |
| Mapper type | Audience |
| Included Custom Audience | `hia-api` |
| Add to access token | On |
| Add to ID token | Off |
| Add to lightweight access token | On |
| Add to token introspection | On |

이 Mapper가 적용되면 Access Token의 `aud`에 `hia-api`가 포함된다.

```json
{
  "aud": [
    "hia-api",
    "account"
  ]
}
```

HIA 백엔드 API는 Access Token 검증 시 다음 항목을 확인해야 한다.

- JWT 서명
- `iss`
- `exp`
- `aud`에 `hia-api` 포함 여부

---

## 11. PKCE 로그인 검증

### 11.1 Code Verifier 생성

```powershell
$randomBytes = New-Object byte[] 32

$rng = [Security.Cryptography.RandomNumberGenerator]::Create()

try {
    $rng.GetBytes($randomBytes)
}
finally {
    $rng.Dispose()
}

$codeVerifier = [Convert]::ToBase64String($randomBytes).
    TrimEnd('=').
    Replace('+', '-').
    Replace('/', '_')
```

길이를 확인한다.

```powershell
$codeVerifier.Length
```

예상 길이:

```text
43
```

### 11.2 Code Challenge 생성

```powershell
$sha256 = [Security.Cryptography.SHA256]::Create()

try {
    $challengeBytes = $sha256.ComputeHash(
        [Text.Encoding]::ASCII.GetBytes($codeVerifier)
    )
}
finally {
    $sha256.Dispose()
}

$codeChallenge = [Convert]::ToBase64String($challengeBytes).
    TrimEnd('=').
    Replace('+', '-').
    Replace('/', '_')
```

길이를 확인한다.

```powershell
$codeChallenge.Length
```

예상 길이:

```text
43
```

### 11.3 로그인 URL 생성

```powershell
$state = [Guid]::NewGuid().ToString("N")
$redirectUri = "http://localhost:5173/"

$authUrl =
    "http://localhost:8180/realms/hia/protocol/openid-connect/auth" +
    "?client_id=hia-web" +
    "&response_type=code" +
    "&scope=openid%20profile%20email" +
    "&redirect_uri=$([Uri]::EscapeDataString($redirectUri))" +
    "&code_challenge=$codeChallenge" +
    "&code_challenge_method=S256" +
    "&state=$state"

Start-Process $authUrl
```

브라우저에서 로그인한다.

프런트엔드 서버가 실행 중이 아니면 로그인 후 `localhost:5173` 연결 실패 화면이 표시될 수 있다. 이 경우에도 주소 표시줄에 `code`와 `state`가 포함되어 있으면 Keycloak 로그인은 성공한 것이다.

Authorization Code는 유효시간이 짧고 한 번만 사용할 수 있으므로 Callback URL을 받은 직후 토큰으로 교환한다.

### 11.4 Callback 검증

브라우저 주소 표시줄의 전체 URL을 복사한다.

```powershell
$callbackUrl = Read-Host "Callback URL"
```

Authorization Code를 추출한다.

```powershell
if ($callbackUrl -match '[?&]code=([^&]+)') {
    $code = [Uri]::UnescapeDataString($Matches[1])
}
else {
    throw "Callback URL에서 Authorization Code를 찾지 못했습니다."
}
```

`state`를 추출하고 처음 생성한 값과 비교한다.

```powershell
if ($callbackUrl -match '[?&]state=([^&]+)') {
    $returnedState = [Uri]::UnescapeDataString($Matches[1])
}
else {
    throw "Callback URL에서 state를 찾지 못했습니다."
}

if ($returnedState -ne $state) {
    throw "state 값이 일치하지 않습니다."
}
```

### 11.5 Authorization Code를 토큰으로 교환

```powershell
$tokenResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8180/realms/hia/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type    = "authorization_code"
        client_id     = "hia-web"
        code          = $code
        redirect_uri  = $redirectUri
        code_verifier = $codeVerifier
    }
```

토큰 메타데이터를 확인한다.

```powershell
$tokenResponse |
    Select-Object token_type, expires_in, refresh_expires_in, scope
```

예상되는 주요 값:

```text
token_type : Bearer
expires_in : 300
scope      : openid email profile
```

Access Token과 Refresh Token은 Credential과 동일하게 취급한다. 토큰 전체 문자열을 로그, 문서, 이슈 또는 PR에 첨부하지 않는다.

---

## 12. JWT Claim 확인

다음 함수로 JWT Payload를 디코딩한다.

```powershell
function ConvertFrom-Base64Url([string]$value) {
    $base64 = $value.Replace('-', '+').Replace('_', '/')

    switch ($base64.Length % 4) {
        2 { $base64 += "==" }
        3 { $base64 += "=" }
        1 { throw "잘못된 Base64URL 문자열입니다." }
    }

    [Text.Encoding]::UTF8.GetString(
        [Convert]::FromBase64String($base64)
    )
}
```

Access Token Payload를 확인한다.

```powershell
$payloadPart = $tokenResponse.access_token.Split('.')[1]

$jwtPayload = ConvertFrom-Base64Url $payloadPart |
    ConvertFrom-Json

$jwtPayload |
    Select-Object `
        sub,
        email,
        email_verified,
        name,
        preferred_username,
        iss,
        aud,
        azp,
        iat,
        exp
```

확인할 항목:

| Claim | 예상값 또는 조건 |
|---|---|
| `sub` | Keycloak 사용자 고유 ID |
| `iss` | `http://localhost:8180/realms/hia` |
| `aud` | `hia-api` 포함 |
| `azp` | `hia-web` |
| `exp - iat` | `300` |
| `email_verified` | 사용자의 실제 이메일 인증 상태 |

`email_verified`는 테스트를 위해 무조건 `true`로 변경하지 않는다. 실제 이메일 인증 여부를 나타내야 한다.

---

## 13. 잘못된 요청 거부 확인

### PKCE가 없는 요청

`code_challenge` 없이 로그인 요청을 보내면 Keycloak이 요청을 거부해야 한다.

```powershell
$negativeState = [Guid]::NewGuid().ToString("N")

$noPkceUrl =
    "http://localhost:8180/realms/hia/protocol/openid-connect/auth" +
    "?client_id=hia-web" +
    "&response_type=code" +
    "&scope=openid" +
    "&redirect_uri=$([Uri]::EscapeDataString($redirectUri))" +
    "&state=$negativeState"

Start-Process $noPkceUrl
```

예상 결과:

```text
error=invalid_request
```

### `plain` PKCE 요청

```powershell
$plainState = [Guid]::NewGuid().ToString("N")
$plainChallenge = "A" * 43

$plainPkceUrl =
    "http://localhost:8180/realms/hia/protocol/openid-connect/auth" +
    "?client_id=hia-web" +
    "&response_type=code" +
    "&scope=openid" +
    "&redirect_uri=$([Uri]::EscapeDataString($redirectUri))" +
    "&code_challenge=$plainChallenge" +
    "&code_challenge_method=plain" +
    "&state=$plainState"

Start-Process $plainPkceUrl
```

예상 결과:

```text
error=invalid_request
error_description=Invalid parameter: code challenge method is not matching the configured one
```

### 등록되지 않은 Redirect URI

```powershell
$invalidRedirect = "http://localhost:9999/"

$invalidRedirectUrl =
    "http://localhost:8180/realms/hia/protocol/openid-connect/auth" +
    "?client_id=hia-web" +
    "&response_type=code" +
    "&scope=openid" +
    "&redirect_uri=$([Uri]::EscapeDataString($invalidRedirect))" +
    "&code_challenge=$codeChallenge" +
    "&code_challenge_method=S256" +
    "&state=$([Guid]::NewGuid().ToString('N'))"

Start-Process $invalidRedirectUrl
```

등록되지 않은 Redirect URI이므로 Keycloak이 요청을 거부해야 한다.

---

## 14. 서비스 종료

컨테이너를 종료하되 데이터를 유지하려면 다음 명령을 사용한다.

```powershell
docker compose down
```

컨테이너만 일시 정지하려면 다음 명령을 사용할 수 있다.

```powershell
docker compose stop
```

다시 시작한다.

```powershell
docker compose start
```

---

## 15. 로컬 데이터 완전 초기화

> 주의: 다음 명령은 User Service PostgreSQL과 Keycloak PostgreSQL의 로컬 데이터를 모두 삭제한다.

```powershell
docker compose down --volumes
```

초기화 후 다시 시작한다.

```powershell
docker compose up -d
```

깨끗한 Keycloak DB에서는 `keycloak/import/hia-realm.json`을 읽어 `hia` Realm이 자동으로 생성된다.

중요한 데이터가 있는 환경에서는 `--volumes`를 실행하지 않는다.

---

## 16. Realm Import 동작

Realm 설정 파일:

```text
keycloak/import/hia-realm.json
```

컨테이너 내부 경로:

```text
/opt/keycloak/data/import
```

Compose 설정:

```yaml
command: start-dev --import-realm

volumes:
  - ./keycloak/import:/opt/keycloak/data/import:ro
```

`--import-realm`은 새로운 환경의 초기 설정을 위한 기능이다.

기존 DB에 `hia` Realm이 있으면 Import가 자동으로 건너뛰어진다. 따라서 `hia-realm.json`을 수정한 후 Keycloak을 재시작하는 것만으로 기존 Realm이 갱신되지는 않는다.

기존 Realm 설정 변경은 다음 중 하나로 관리해야 한다.

- Keycloak Admin Console
- Keycloak Admin REST API
- 별도의 설정 마이그레이션
- 데이터 삭제 후 재생성이 허용되는 로컬 환경에서만 전체 초기화

---

## 17. Realm Export 보안 정책

현재 Realm JSON은 다음 원칙으로 관리한다.

- 사용자 내보내기 제외
- 사용자 Credential 제외
- 실제 Client Secret 제외
- HMAC/AES Secret은 환경변수 자리표시자로 치환
- 실제 Secret은 `.env`에만 저장

Realm JSON에서 사용하는 자리표시자:

```json
"${HIA_KC_HMAC_SECRET}"
```

```json
"${HIA_KC_AES_SECRET}"
```

커밋 전 다음을 확인한다.

```powershell
$realm = Get-Content `
    ".\keycloak\import\hia-realm.json" `
    -Raw |
    ConvertFrom-Json

$keyProviders = @(
    $realm.components.'org.keycloak.keys.KeyProvider'
)

[PSCustomObject]@{
    HasUsers = $realm.PSObject.Properties.Name -contains "users"

    ClientSecretCount = @(
        $realm.clients |
        Where-Object {
            -not [string]::IsNullOrWhiteSpace($_.secret)
        }
    ).Count

    HmacSecretReplaced = (
        (
            $keyProviders |
            Where-Object providerId -eq "hmac-generated"
        ).config.secret -join ""
    ) -eq '${HIA_KC_HMAC_SECRET}'

    AesSecretReplaced = (
        (
            $keyProviders |
            Where-Object providerId -eq "aes-generated"
        ).config.secret -join ""
    ) -eq '${HIA_KC_AES_SECRET}'
}
```

예상 결과:

```text
HasUsers           : False
ClientSecretCount  : 0
HmacSecretReplaced : True
AesSecretReplaced  : True
```

---

## 18. 현재 범위와 향후 변경

현재 로컬 Realm은 Keycloak 자체 회원가입을 허용한다.

```text
User registration: On
```

향후 User Service가 회원가입을 통제하는 방식으로 변경하면 다음 작업이 필요하다.

- Keycloak 자체 회원가입 비활성화
- User Service용 Confidential Client 생성
- Service Account 활성화
- 최소 사용자 관리 권한 부여
- User Service에서 Keycloak Admin API 연동
- Keycloak `sub`와 User Service 회원 ID 연결
- 회원 생성 실패에 대한 보상 처리

회원가입 경로가 변경되어도 다음 설정은 계속 필요하다.

- `hia` Realm
- `hia-web`
- Authorization Code Flow
- PKCE `S256`
- Redirect URI
- 토큰과 세션 설정
- `hia-api` Audience
- Realm 서명 및 암호화 키
