package com.coffrefort.client;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client d'API pour communiquer avec le backend.
 * Gère l'authentification JWT et les appels HTTP.
 */
public class ApiClient {
    private String baseUrl = "http://localhost:8888/"; // URL de base de l'API
    private String authToken; // JWT token
    
    private final OkHttpClient httpClient;
    private final ObjectMapper jsonMapper;

    public ApiClient() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.jsonMapper = new ObjectMapper();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Authentifie l'utilisateur et récupère le token JWT
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @return true si la connexion réussit, false sinon
     * @throws IOException En cas d'erreur réseau
     */
    public boolean login(String email, String password) throws IOException {
        // Créer le corps de la requête JSON
        String jsonBody = jsonMapper.writeValueAsString(new LoginRequest(email, password));
        
        RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(baseUrl + "auth/login")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Échec de la connexion: " + response.code() + " - " + errorBody);
            }
            
            // Parser la réponse JSON
            String responseBody = response.body().string();
            JsonNode jsonResponse = jsonMapper.readTree(responseBody);
            
            // Extraire le token de la réponse
            if (jsonResponse.has("token")) {
                this.authToken = jsonResponse.get("token").asText();
                return true;
            } else {
                throw new IOException("Réponse invalide: token manquant");
            }
        }
    }

    public boolean isAuthenticated() {
        return authToken != null && !authToken.isEmpty();
    }

    public String getAuthToken() {
        return authToken;
    }

    public void clearToken() {
        this.authToken = null;
    }

    /**
     * Effectue une déconnexion (invalide le token côté serveur si l'API le supporte)
     */
    public void logout() throws IOException {
        if (!isAuthenticated()) {
            return;
        }
        
        // TODO: Si l'API a un endpoint de déconnexion, l'appeler ici
        // Request request = new Request.Builder()
        //     .url(baseUrl + "auth/logout")
        //     .post(RequestBody.create("", null))
        //     .addHeader("Authorization", "Bearer " + authToken)
        //     .build();
        // httpClient.newCall(request).execute();
        
        // Nettoyer le token localement
        clearToken();
    }

    /**
     * Récupère la liste des fichiers depuis l'API
     * GET /files
     * @return Liste des fichiers
     * @throws IOException En cas d'erreur réseau
     */
    public List<FileEntry> listFiles() throws IOException {
        if (!isAuthenticated()) {
            throw new IOException("Non authentifié. Veuillez vous connecter d'abord.");
        }
        
        Request request = new Request.Builder()
            .url(baseUrl + "files")
            .get()
            .addHeader("Authorization", "Bearer " + authToken)
            .addHeader("Accept", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Échec de récupération des fichiers: " + response.code() + " - " + errorBody);
            }
            
            // Parser la réponse JSON
            String responseBody = response.body().string();
            JsonNode jsonResponse = jsonMapper.readTree(responseBody);
            
            List<FileEntry> files = new ArrayList<>();
            
            // Si la réponse est un tableau
            if (jsonResponse.isArray()) {
                for (JsonNode fileNode : jsonResponse) {
                    files.add(parseFileEntry(fileNode));
                }
            }
            // Si la réponse est un objet avec un champ "files" ou "data"
            else if (jsonResponse.has("files")) {
                JsonNode filesArray = jsonResponse.get("files");
                for (JsonNode fileNode : filesArray) {
                    files.add(parseFileEntry(fileNode));
                }
            } else if (jsonResponse.has("data")) {
                JsonNode filesArray = jsonResponse.get("data");
                for (JsonNode fileNode : filesArray) {
                    files.add(parseFileEntry(fileNode));
                }
            }
            
            return files;
        }
    }

    /**
     * Parse un nœud JSON en FileEntry
     */
    private FileEntry parseFileEntry(JsonNode fileNode) {
        String name = fileNode.has("filename") ? fileNode.get("filename").asText() : 
                     fileNode.has("original_name") ? fileNode.get("original_name").asText() : "Sans nom";
        
        long size = fileNode.has("size") ? fileNode.get("size").asLong() : 0L;
        
        // Parser la date (plusieurs formats possibles)
        Instant updatedAt = Instant.now();
        if (fileNode.has("uploaded_at")) {
            try {
                
            } catch (Exception e) {
                // Si le parsing échoue, garder la date actuelle
            }
        } else if (fileNode.has("created_at")) {
            try {
                updatedAt = Instant.parse(fileNode.get("created_at").asText());
            } catch (Exception e) {
                // Si le parsing échoue, garder la date actuelle
            }
        }
        
        // Récupérer la version actuelle (si disponible)
        int currentVersion = fileNode.has("current_version") ? fileNode.get("current_version").asInt() : 
                           fileNode.has("version") ? fileNode.get("version").asInt() : 1;
        
        return FileEntry.of(name, size, updatedAt, currentVersion);
    }

    /**
     * Retourne une arborescence factice de dossiers/fichiers avec versions.
         * Remplit l'arbre principal en utilisant `listFiles()` (réellement récupère les fichiers).
         * Ne lève pas d'exception : en cas d'erreur réseau on retourne une liste vide.
     */
    public List<NodeItem> listRoot() {
        List<NodeItem> root = new ArrayList<>();
        try {
            // Récupérer la liste réelle des fichiers via listFiles()
            List<FileEntry> files = listFiles();

            // Placer tous les fichiers à la racine (UI actuelle gère un seul niveau)
            NodeItem racine = NodeItem.folder("Racine");
            racine.withFiles(files);
            root.add(racine);
        } catch (IOException e) {
            // En cas d'erreur, logguer et retourner une liste vide (UI montrera un arbre vide)
            System.err.println("Erreur lors de la récupération des fichiers pour listRoot(): " + e.getMessage());
            e.printStackTrace();
        }

        return root;
    }

    /**
     * Quota simulé: 2 Go max, 350 Mo utilisés.
     * TODO: Remplacer par un vrai appel API
     */
    public Quota getQuota() {
        return new Quota(350L * 1024 * 1024, 2L * 1024 * 1024 * 1024);
    }

    /**
     * Classe interne pour la requête de login
     */
    private static class LoginRequest {
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
}