# Gestion des stages - DTA Alliance

Application Spring Boot de gestion des candidatures et des stages.

## Prerequis Docker

- Docker Desktop avec Docker Compose v2
- Les ports `8080` ou celui defini dans `.env` doivent etre disponibles
- Aucune autre instance locale de l'application ne doit utiliser la base H2 du dossier `data`

## Configuration

Creer le fichier local `.env` a partir du modele :

```powershell
Copy-Item .env.example .env
```

Renseigner au minimum `MAIL_USERNAME` et `MAIL_PASSWORD` pour activer l'envoi
d'emails. Le fichier `.env` est ignore par Git et ne doit pas etre partage.

## Demarrage

Depuis le dossier contenant `compose.yaml` :

```powershell
docker compose up --build -d
```

L'application est ensuite disponible sur :

```text
http://localhost:8080
```

## Acces depuis un telephone ou un autre ordinateur

Spring Boot ecoute sur toutes les interfaces reseau (`0.0.0.0`) et Docker
publie le port `8080`. Le telephone et le PC doivent etre connectes au meme
reseau Wi-Fi.

Afficher les adresses IPv4 du PC :

```powershell
Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object {
        $_.IPAddress -notlike "127.*" -and
        $_.IPAddress -notlike "169.254*"
    } |
    Select-Object InterfaceAlias, IPAddress
```

Utiliser l'adresse de l'interface reellement connectee. Par exemple, si
l'adresse Wi-Fi affichee est `192.168.1.37`, ouvrir sur le telephone :

```text
http://192.168.1.37:8080
```

Autoriser une seule fois les connexions entrantes sur le port `8080` dans un
terminal PowerShell ouvert en tant qu'administrateur :

```powershell
New-NetFirewallRule `
    -DisplayName "DTA Alliance - Spring Boot 8080" `
    -Direction Inbound `
    -Protocol TCP `
    -LocalPort 8080 `
    -Action Allow `
    -Profile Private
```

Verifier que le reseau Windows est defini comme `Prive`. Le profil prive evite
d'exposer inutilement le serveur sur les reseaux publics. Si l'adresse IP du
PC change apres une reconnexion au Wi-Fi, reprendre l'adresse affichee par la
commande precedente.

Afficher les journaux :

```powershell
docker compose logs -f gestion-stages
```

Arreter l'application :

```powershell
docker compose down
```

Reconstruire apres une modification du code :

```powershell
docker compose up --build -d
```

## Donnees persistantes

Le conteneur utilise deux montages :

- `./data:/app/data` pour la base H2 ;
- `./uploads:/app/uploads` pour les documents televerses.

La commande `docker compose down` ne supprime donc ni la base ni les documents.
Ne lancez pas simultanement l'application avec Maven et Docker sur la meme base
H2, car H2 verrouille son fichier lorsqu'il est utilise.

## Commandes de verification

Compilation locale :

```powershell
.\mvnw.cmd -DskipTests package
```

Tests :

```powershell
.\mvnw.cmd test
```

Verification de la configuration Compose :

```powershell
docker compose config
```

## Livraison

Les fichiers necessaires a la conteneurisation sont :

- `Dockerfile`
- `compose.yaml`
- `.dockerignore`
- `.env.example`

Ne jamais livrer `.env`, car il contient les secrets SMTP.
