#!/usr/bin/env python3
"""
Script pour scanner les vulnérabilités du projet avec Semgrep
"""

import subprocess
import sys
import os
from pathlib import Path


def run_semgrep_scan():
    """
    Lance un scan Semgrep sur le projet
    """
    try:
        # Trouver le chemin de l'exécutable pysemgrep
        user_scripts_dir = Path.home() / ".local" / "bin"
        appdata_scripts_dir = Path(os.environ.get("APPDATA", "")) / "Python" / "Python313" / "Scripts"
        
        pysemgrep_exec = None
        
        # Chercher l'exécutable pysemgrep dans les répertoires usuels
        for scripts_dir in [user_scripts_dir, appdata_scripts_dir]:
            pysemgrep_path = scripts_dir / "pysemgrep.exe"
            if pysemgrep_path.exists():
                pysemgrep_exec = str(pysemgrep_path)
                break
        
        if pysemgrep_exec:
            # Ajouter le répertoire des scripts à la variable PATH
            scripts_dir = os.path.dirname(pysemgrep_exec)
            os.environ["PATH"] = scripts_dir + os.pathsep + os.environ.get("PATH", "")
            
            # Lancer le scan avec les règles personnalisées
            cmd = [pysemgrep_exec, "scan", "--config=auto", "--config=semgrep_rules.yml", "."]
        else:
            # Si l'exécutable n'est pas trouvé, essayer avec python -m
            cmd = [sys.executable, "-m", "semgrep", "scan", "--config=auto", "--config=semgrep_rules.yml", "."]
        
        print("Lancement du scan de sécurité avec Semgrep...")
        print(f"Commande: {' '.join(cmd)}")
        print("Cela peut prendre quelques minutes...")
        
        # Lance le processus
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            encoding='utf-8'  # Spécifier l'encodage pour éviter les erreurs de décodage
        )
        
        # Afficher la sortie
        print("\n--- RÉSULTATS DU SCAN ---")
        print(result.stdout)
        
        if result.stderr:
            print("\n--- ERREURS ---")
            print(result.stderr)
        
        print(f"\nCode de retour: {result.returncode}")
        
        return result.returncode
        
    except KeyboardInterrupt:
        print("\nScan interrompu par l'utilisateur...")
        return 1
    except Exception as e:
        print(f"Erreur lors du lancement du scan Semgrep : {e}")
        return 1


def run_semgrep_with_mcp():
    """
    Fonction alternative qui utiliserait le serveur MCP pour scanner
    """
    print("Pour utiliser le serveur MCP Semgrep directement dans un IDE prenant en charge MCP :")
    print("1. Assurez-vous que le serveur MCP est en cours d'exécution : python semgrep_mcp_server.py")
    print("2. Dans votre IDE (comme Cursor ou VS Code avec extension MCP), les outils Semgrep seront disponibles")
    print("3. Les outils disponibles incluent :")
    print("   - security_check: Scan de sécurité")
    print("   - semgrep_scan: Scan avec une configuration spécifique")
    print("   - get_abstract_syntax_tree: Obtenir l'arbre syntaxique du code")
    print("   - supported_languages: Langages supportés")
    print("   - semgrep_rule_schema: Schéma des règles Semgrep")


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--mcp-info":
        run_semgrep_with_mcp()
    else:
        exit_code = run_semgrep_scan()
        if exit_code == 0:
            print("\nScan terminé avec succès. Aucune vulnérabilité critique détectée.")
        elif exit_code == 1:
            print(f"\nScan terminé avec code de retour {exit_code}. Des vulnérabilités ont été détectées.")
        else:
            print(f"\nScan terminé avec code de retour {exit_code}.")
        
        print("\nPour utiliser le serveur MCP Semgrep dans votre IDE, exécutez : python semgrep_mcp_server.py --mcp-info")