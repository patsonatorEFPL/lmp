#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de vérification de connexion à la base de données MySQL Railway
Pour l'application Spring Boot LMP
"""

import mysql.connector
import sys
import os
from urllib.parse import urlparse

def parse_database_url(database_url):
    """Parse l'URL de base de données Railway"""
    try:
        parsed = urlparse(database_url)
        return {
            'host': parsed.hostname,
            'port': parsed.port or 3306,
            'user': parsed.username,
            'password': parsed.password,
            'database': parsed.path[1:]  # Enlever le slash initial
        }
    except Exception as e:
        print(f"❌ Erreur parsing URL: {e}")
        return None

def test_connection(db_config):
    """Teste la connexion à la base de données"""
    try:
        print("🔍 Test de connexion à la base de données...")
        print(f"Host: {db_config['host']}")
        print(f"Port: {db_config['port']}")
        print(f"Database: {db_config['database']}")
        print(f"User: {db_config['user']}")
        
        # Connexion
        connection = mysql.connector.connect(
            host=db_config['host'],
            port=db_config['port'],
            user=db_config['user'],
            password=db_config['password'],
            database=db_config['database'],
            connection_timeout=10,
            autocommit=True
        )
        
        if connection.is_connected():
            print("✅ Connexion réussie !")
            
            # Test de requête simple
            cursor = connection.cursor()
            cursor.execute("SELECT VERSION()")
            version = cursor.fetchone()
            print(f"📊 Version MySQL: {version[0]}")
            
            # Vérifier les tables existantes
            cursor.execute("SHOW TABLES")
            tables = cursor.fetchall()
            print(f"📋 Nombre de tables: {len(tables)}")
            
            # Vérifier la table flyway_schema_history
            cursor.execute("SHOW TABLES LIKE 'flyway_schema_history'")
            flyway_exists = cursor.fetchone()
            if flyway_exists:
                cursor.execute("SELECT COUNT(*) FROM flyway_schema_history")
                migration_count = cursor.fetchone()[0]
                print(f"🔄 Migrations Flyway: {migration_count}")
            else:
                print("⚠️  Table flyway_schema_history non trouvée")
            
            # Vérifier la table users
            cursor.execute("SHOW TABLES LIKE 'users'")
            users_exists = cursor.fetchone()
            if users_exists:
                cursor.execute("SELECT COUNT(*) FROM users")
                user_count = cursor.fetchone()[0]
                print(f"👥 Nombre d'utilisateurs: {user_count}")
            else:
                print("⚠️  Table users non trouvée")
            
            cursor.close()
            connection.close()
            return True
            
    except mysql.connector.Error as e:
        print(f"❌ Erreur MySQL: {e}")
        return False
    except Exception as e:
        print(f"❌ Erreur générale: {e}")
        return False

def main():
    """Fonction principale"""
    print("🚀 Script de test de connexion Railway MySQL")
    print("=" * 50)
    
    # Récupérer l'URL de la base de données
    database_url = os.getenv('DATABASE_URL')
    
    if not database_url:
        print("❌ Variable d'environnement DATABASE_URL non définie")
        print("\nUtilisation:")
        print("export DATABASE_URL='mysql://user:password@host:port/database'")
        print("python3 scripts/test-db-connection.py")
        sys.exit(1)
    
    # Parser l'URL
    db_config = parse_database_url(database_url)
    if not db_config:
        sys.exit(1)
    
    # Tester la connexion
    success = test_connection(db_config)
    
    if success:
        print("\n✅ Test de connexion réussi !")
        print("📝 La base de données est prête pour le déploiement")
        sys.exit(0)
    else:
        print("\n❌ Test de connexion échoué !")
        print("🔧 Vérifiez vos paramètres de connexion")
        sys.exit(1)

if __name__ == "__main__":
    main()