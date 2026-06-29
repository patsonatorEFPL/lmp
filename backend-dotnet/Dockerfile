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
COPY --from=build /app ./

ENV ASPNETCORE_URLS=http://0.0.0.0:8080 \
    DOTNET_NOLOGO=1 \
    DOTNET_CLI_TELEMETRY_OPTOUT=1
EXPOSE 8080

ENTRYPOINT ["dotnet", "Lmp.Api.dll"]
