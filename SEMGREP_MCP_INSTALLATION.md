# Installation du serveur MCP Semgrep

Ce document explique comment installer et utiliser le serveur Model Context Protocol (MCP) pour Semgrep dans ce projet.

## Description

Le serveur MCP Semgrep permet d'intégrer Semgrep avec des outils prenant en charge le Model Context Protocol (MCP) comme Cursor, VS Code, Claude, etc. Il permet d'analyser le code pour détecter les vulnérabilités de sécurité et d'autres problèmes.

## Installation

L'installation a été effectuée via pip :

```bash
pip install semgrep
```

## Configuration

Le serveur MCP est configuré dans le fichier `.vscode/mcp.json` pour une utilisation avec VS Code :

```json
{
  "servers": {
    "semgrep": {
      "command": "semgrep",
      "args": ["mcp"]
    }
  }
}
```

## Utilisation

### Via le script Python

Pour lancer le serveur MCP Semgrep :

```bash
python semgrep_mcp_server.py
```

Le serveur fonctionnera en mode stdio (entrée/sortie standard), ce qui est approprié pour les intégrations MCP.

### Fonctionnalités

Une fois configuré, le serveur MCP Semgrep fournit :

- **Outils** : 
 - `security_check` : Analyse le code pour détecter les vulnérabilités de sécurité
  - `semgrep_scan` : Analyse les fichiers de code pour les vulnérabilités de sécurité avec une configuration donnée
  - `semgrep_scan_with_custom_rule` : Analyse les fichiers de code en utilisant une règle Semgrep personnalisée
  - `get_abstract_syntax_tree` : Sort l'arbre syntaxique abstrait (AST) du code
  - `supported_languages` : Retourne la liste des langages supportés par Semgrep
  - `semgrep_rule_schema` : Récupère le dernier schéma JSON de règle Semgrep

- **Ressources** :
  - `semgrep://rule/schema` : Spécification de la syntaxe YAML de règle Semgrep en utilisant le schéma JSON
  - `semgrep://rule/{rule_id}/yaml` : Règle Semgrep complète au format YAML depuis le registre Semgrep

## Intégration avec les IDE

### VS Code

Le serveur MCP est automatiquement détecté par VS Code s'il est installé via l'extension MCP. Le fichier `.vscode/mcp.json` configure l'IDE pour utiliser le serveur Semgrep.

### Cursor

Ajoutez le bloc JSON suivant à votre fichier de configuration `~/.cursor/mcp.json` ou `.cursor/mcp.json` :

```json
{
  "mcpServers": {
    "semgrep": {
      "command": "semgrep",
      "args": ["mcp"]
    }
  }
}
```

## Dépannage

### Commande 'semgrep' non trouvée

Si la commande `semgrep` n'est pas reconnue, vous pouvez :

1. Utiliser le script Python fourni : `python semgrep_mcp_server.py`
2. Ajouter le répertoire d'installation de Python à votre PATH
3. Utiliser `python -m semgrep` à la place de `semgrep`

### Problèmes d'intégration MCP

Assurez-vous que votre IDE prend en charge le Model Context Protocol et que les paramètres MCP sont correctement configurés.