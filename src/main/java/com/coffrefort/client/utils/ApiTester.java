package com.coffrefort.client.utils;

import com.coffrefort.client.ApiClient;

import java.io.File;
import java.util.Scanner;

/**
 * Classe utilitaire pour tester manuellement les endpoints de l'API
 * Exécutez cette classe pour faire des tests rapides
 */
public class ApiTester {
    
    public static void main(String[] args) {
        ApiClient apiClient = new ApiClient();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=================================");
        System.out.println("   API TESTER - Coffre-fort");
        System.out.println("=================================\n");
        
        // Configuration de l'URL
        System.out.print("URL du serveur [http://localhost:8888/]: ");
        String url = scanner.nextLine().trim();
        if (!url.isEmpty()) {
            apiClient.setBaseUrl(url);
        }
        System.out.println("Base URL: " + apiClient.getBaseUrl());
        
        // Login
        System.out.println("\n--- AUTHENTIFICATION ---");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        
        System.out.print("Mot de passe: ");
        String password = scanner.nextLine().trim();
        
        try {
            System.out.println("\nConnexion en cours...");
            boolean success = apiClient.login(email, password);
            
            if (success) {
                System.out.println("✓ Connexion réussie !");
                System.out.println("Token: " + apiClient.getAuthToken().substring(0, 20) + "...");
            } else {
                System.out.println("✗ Échec de la connexion");
                return;
            }
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la connexion: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // Menu principal
        while (true) {
            System.out.println("\n=================================");
            System.out.println("MENU PRINCIPAL");
            System.out.println("=================================");
            System.out.println("1. Lister les dossiers");
            System.out.println("2. Créer un dossier");
            System.out.println("3. Lister les fichiers");
            System.out.println("4. Upload un fichier");
            System.out.println("5. Afficher l'arborescence complète");
            System.out.println("0. Quitter");
            System.out.print("\nChoix: ");
            
            String choice = scanner.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1":
                        testListFolders(apiClient);
                        break;
                    case "2":
                        testCreateFolder(apiClient, scanner);
                        break;
                    case "3":
                        testListFiles(apiClient);
                        break;
                    case "4":
                        testUploadFile(apiClient, scanner);
                        break;
                    case "5":
                        testListRoot(apiClient);
                        break;
                    case "0":
                        System.out.println("Au revoir !");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Choix invalide");
                }
            } catch (Exception e) {
                System.err.println("✗ Erreur: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private static void testListFolders(ApiClient apiClient) {
        System.out.println("\n--- LISTE DES DOSSIERS ---");
        System.out.println("Note: Cette fonctionnalité nécessite l'accès à la classe interne FolderDto");
        try {
            System.out.println("Utilisez testListRoot() pour voir l'arborescence complète");
        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testCreateFolder(ApiClient apiClient, Scanner scanner) {
        System.out.println("\n--- CRÉER UN DOSSIER ---");
        System.out.print("Nom du dossier: ");
        String name = scanner.nextLine().trim();
        
        System.out.print("ID du dossier parent (vide pour racine): ");
        String parentIdStr = scanner.nextLine().trim();
        Integer parentId = parentIdStr.isEmpty() ? null : Integer.parseInt(parentIdStr);
        
        try {
            Integer folderId = apiClient.createFolder(name, parentId);
            System.out.println("✓ Dossier créé avec succès !");
            System.out.println("ID du dossier: " + folderId);
        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testListFiles(ApiClient apiClient) {
        System.out.println("\n--- LISTE DES FICHIERS ---");
        System.out.println("Note: Cette fonctionnalité nécessite l'accès à la classe interne FileDto");
        try {
            System.out.println("Utilisez testListRoot() pour voir l'arborescence complète");
        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testUploadFile(ApiClient apiClient, Scanner scanner) {
        System.out.println("\n--- UPLOAD DE FICHIER ---");
        System.out.print("Chemin du fichier: ");
        String filePath = scanner.nextLine().trim();
        
        System.out.print("ID du dossier de destination (vide pour racine): ");
        String folderIdStr = scanner.nextLine().trim();
        Integer folderId = folderIdStr.isEmpty() ? null : Integer.parseInt(folderIdStr);
        
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("✗ Fichier introuvable: " + filePath);
            return;
        }
        
        try {
            System.out.println("Upload en cours...");
            Integer fileId = apiClient.uploadFile(file, folderId);
            System.out.println("✓ Fichier uploadé avec succès !");
            System.out.println("ID du fichier: " + fileId);
        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testListRoot(ApiClient apiClient) {
        System.out.println("\n--- ARBORESCENCE COMPLÈTE ---");
        try {
            var roots = apiClient.listRoot();
            System.out.println("Dossiers racine: " + roots.size());
            
            for (var node : roots) {
                printNode(node, 0);
            }
        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printNode(com.coffrefort.client.model.NodeItem node, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + "📁 " + node.getName() + 
                         " [ID: " + node.getId() + "]" +
                         " (" + node.getFiles().size() + " fichiers, " + 
                         node.getChildren().size() + " sous-dossiers)");
        
        // Afficher les fichiers
        for (var file : node.getFiles()) {
            System.out.println(indent + "  📄 " + file.getName() + 
                             " (" + formatSize(file.getSize()) + ")");
        }
        
        // Afficher les sous-dossiers récursivement
        for (var child : node.getChildren()) {
            printNode(child, depth + 1);
        }
    }
    
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        double v = bytes;
        String[] units = {"Ko", "Mo", "Go", "To"};
        int i = -1;
        while (v >= 1024 && i < units.length - 1) { 
            v /= 1024.0; 
            i++; 
        }
        return String.format("%.2f %s", v, units[i]);
    }
}