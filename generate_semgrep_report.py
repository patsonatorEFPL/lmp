#!/usr/bin/env python3
"""
Script pour générer un rapport à partir des résultats du scan Semgrep
"""

import json
import subprocess
import sys
import os
from pathlib import Path


def run_semgrep_scan_with_json_output():
    """
    Lance un scan Semgrep et sauvegarde les résultats au format JSON
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
            
            # Lancer le scan avec les règles personnalisées et sortie JSON
            cmd = [pysemgrep_exec, "scan", "--config=auto", "--config=semgrep_rules.yml", "--json", "."]
        else:
            # Si l'exécutable n'est pas trouvé, essayer avec python -m (bien que déprécié)
            cmd = [sys.executable, "-m", "semgrep", "scan", "--config=auto", "--config=semgrep_rules.yml", "--json", "."]
        
        print("Lancement du scan de sécurité avec Semgrep (sortie JSON)...")
        print(f"Commande: {' '.join(cmd)}")
        print("Cela peut prendre quelques minutes...")
        
        # Lance le processus
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            encoding='utf-8'  # Spécifier l'encodage pour éviter les erreurs de décodage
        )
        
        # Sauvegarder les résultats bruts
        with open('semgrep_scan_results.json', 'w', encoding='utf-8') as f:
            f.write(result.stdout)
        
        # Afficher les erreurs s'il y en a
        if result.stderr:
            print("\n--- ERREURS ---")
            print(result.stderr)
        
        print(f"\nCode de retour: {result.returncode}")
        
        # Traiter les résultats JSON
        try:
            scan_results = json.loads(result.stdout)
            generate_report(scan_results)
        except json.JSONDecodeError:
            print("Impossible de traiter les résultats JSON du scan.")
            if result.returncode == 0:
                print("Le scan s'est terminé avec succès mais les résultats ne sont pas au format JSON.")
            else:
                print("Le scan a échoué.")
        
        return result.returncode
        
    except KeyboardInterrupt:
        print("\nScan interrompu par l'utilisateur...")
        return 1
    except Exception as e:
        print(f"Erreur lors du lancement du scan Semgrep : {e}")
        return 1


def generate_report(scan_results):
    """
    Génère un rapport lisible à partir des résultats JSON du scan
    """
    if "results" not in scan_results:
        print("Aucun résultat de scan trouvé dans les données JSON.")
        return
    
    results = scan_results["results"]
    errors = scan_results.get("errors", [])
    
    print("\n" + "="*60)
    print("RAPPORT DE VULNÉRABILITÉS SEMGREP")
    print("="*60)
    
    if results:
        print(f"\nNombre total de vulnérabilités trouvées: {len(results)}")
        
        # Compter par gravité
        severity_count = {}
        for result in results:
            severity = result.get("extra", {}).get("severity", "UNKNOWN")
            severity_count[severity] = severity_count.get(severity, 0) + 1
        
        print("\nRépartition par gravité:")
        for severity, count in severity_count.items():
            print(f"  {severity}: {count}")
        
        print("\nDétails des vulnérabilités:")
        print("-" * 60)
        
        for i, result in enumerate(results, 1):
            path = result.get("path", "unknown")
            line = result.get("start", {}).get("line", "unknown")
            message = result.get("extra", {}).get("message", "No message")
            severity = result.get("extra", {}).get("severity", "UNKNOWN")
            rule_id = result.get("check_id", "unknown")
            
            print(f"\n{i}. [{severity}] {rule_id}")
            print(f"   Fichier: {path}:{line}")
            print(f"   Message: {message}")
            
            # Afficher le code concerné s'il est disponible
            if "extra" in result and "lines" in result["extra"]:
                print(f"   Code concerné: {result['extra']['lines']}")
    else:
        print("\nAucune vulnérabilité critique détectée.")
    
    if errors:
        print(f"\nErreurs rencontrées lors du scan: {len(errors)}")
        for error in errors:
            print(f"  - {error}")


if __name__ == "__main__":
    exit_code = run_semgrep_scan_with_json_output()
    print(f"\nRapport généré dans 'semgrep_scan_results.json'")
    print("\nPour une analyse détaillée, exécutez : python generate_semgrep_report.py")