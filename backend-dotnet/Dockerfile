# LMP .NET 10 backend — multi-stage build.
# Produces a small ASP.NET runtime image listening on :8080 (same port the
# Angular frontend expects). Maps onto the existing Flyway-migrated schema —
# this backend never runs migrations.

# ---- build ----
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

# Restore first (cached layer) using the manifests.
COPY Directory.Build.props Directory.Packages.props ./
COPY src/Lmp.Domain/Lmp.Domain.csproj          src/Lmp.Domain/
COPY src/Lmp.Application/Lmp.Application.csproj  src/Lmp.Application/
COPY src/Lmp.Infrastructure/Lmp.Infrastructure.csproj src/Lmp.Infrastructure/
COPY src/Lmp.Api/Lmp.Api.csproj                 src/Lmp.Api/
RUN dotnet restore src/Lmp.Api/Lmp.Api.csproj

# Build + publish.
COPY . .
RUN dotnet publish src/Lmp.Api/Lmp.Api.csproj -c Release -o /app --no-restore

# ---- runtime ----
FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app

# libgssapi-krb5-2: Npgsql probes the Kerberos/GSSAPI lib at connection time even
# when the connection uses password auth; without it the runtime logs a noisy
# "Cannot load library libgssapi_krb5.so.2" warning. curl: container HEALTHCHECK.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgssapi-krb5-2 curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app ./

ENV ASPNETCORE_URLS=http://0.0.0.0:8080 \
    DOTNET_NOLOGO=1 \
    DOTNET_CLI_TELEMETRY_OPTOUT=1
EXPOSE 8080

# Same liveness contract as the Java backend (curl -f against the liveness probe).
HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
    CMD curl -f -m 5 http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["dotnet", "Lmp.Api.dll"]
