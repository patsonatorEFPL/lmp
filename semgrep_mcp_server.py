#!/usr/bin/env python3
"""
Script pour lancer le serveur MCP Semgrep
Ce serveur permet d'intégrer Semgrep avec les outils prenant en charge le Model Context Protocol (MCP)
"""

import subprocess
import sys
import os
from pathlib import Path


def run_semgrep_mcp():
    """
    Lance le serveur MCP Semgrep en mode stdio
    """
    try:
        # Trouver le chemin de l'exécutable semgrep dans le répertoire utilisateur
        user_scripts_dir = Path.home() / ".local" / "bin"
        appdata_scripts_dir = Path(os.environ.get("APPDATA", "")) / "Python" / "Python313" / "Scripts"
        
        semgrep_exec = None
        
        # Chercher l'exécutable semgrep dans les répertoires usuels
        for scripts_dir in [user_scripts_dir, appdata_scripts_dir]:
            semgrep_path = scripts_dir / "semgrep.exe"
            if semgrep_path.exists():
                semgrep_exec = str(semgrep_path)
                break
        
        if semgrep_exec:
            # Ajouter le répertoire des scripts à la variable PATH
            scripts_dir = os.path.dirname(semgrep_exec)
            os.environ["PATH"] = scripts_dir + os.pathsep + os.environ.get("PATH", "")
            
            # Lancer avec la commande directe
            cmd = [semgrep_exec, "mcp"]
        else:
            # Si l'exécutable n'est pas trouvé, essayer avec python -m
            cmd = [sys.executable, "-m", "semgrep", "mcp"]
        
        print("Lancement du serveur MCP Semgrep...")
        print(f"Commande: {' '.join(cmd)}")
        
        # Lance le processus
        process = subprocess.Popen(
            cmd,
            stdin=sys.stdin,
            stdout=sys.stdout,
            stderr=sys.stderr
        )
        
        # Attend la fin du processus
        process.wait()
        
    except KeyboardInterrupt:
        print("\nArrêt du serveur MCP Semgrep...")
    except Exception as e:
        print(f"Erreur lors du lancement du serveur MCP Semgrep : {e}")
        sys.exit(1)


if __name__ == "__main__":
    run_semgrep_mcp()