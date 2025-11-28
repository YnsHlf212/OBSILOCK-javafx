package com.coffrefort.client;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String baseUrl = "http://localhost:8888/";
    private String authToken;
    
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

    public boolean login(String email, String password) throws IOException {
        String jsonBody = jsonMapper.writeValueAsString(new LoginRequest(email, password));
        
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        
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
            
            String responseBody = response.body().string();
            JsonNode jsonResponse = jsonMapper.readTree(responseBody);
            
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

    public void logout() throws IOException {
        clearToken();
    }

    /**
     * Récupère la liste des dossiers depuis l'API
     * GET /folders
     */
    public List<FolderDto> listFolders() throws IOException {
        if (!isAuthenticated()) {
            throw new IOException("Non authentifié. Veuillez vous connecter d'abord.");
        }
        
        Request request = new Request.Builder()
            .url(baseUrl + "folders")
            .get()
            .addHeader("Authorization", "Bearer " + authToken)
            .addHeader("Accept", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Échec de récupération des dossiers: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            JsonNode jsonResponse = jsonMapper.readTree(responseBody);
            
            List<FolderDto> folders = new ArrayList<>();
            
            if (jsonResponse.isArray()) {
                for (JsonNode folderNode : jsonResponse) {
                    folders.add(parseFolderDto(folderNode));
                }
            } else if (jsonResponse.has("folders") || jsonResponse.has("data")) {
                JsonNode foldersArray = jsonResponse.has("folders") ? 
                    jsonResponse.get("folders") : jsonResponse.get("data");
                for (JsonNode folderNode : foldersArray) {
                    folders.add(parseFolderDto(folderNode));
                }
            }
            
            return folders;
        }
    }

    /**
     * Parse un nœud JSON en FolderDto
     */
    private FolderDto parseFolderDto(JsonNode node) {
        FolderDto folder = new FolderDto();
        folder.id = node.has("id") ? node.get("id").asInt() : null;
        folder.name = node.has("name") ? node.get("name").asText() : "Sans nom";
        folder.parentId = node.has("parent_id") && !node.get("parent_id").isNull() ? 
            node.get("parent_id").asInt() : null;
        return folder;
    }

    /**
     * Récupère la liste des fichiers depuis l'API
     * GET /files
     */
    public List<FileDto> listFiles() throws IOException {
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
            
            String responseBody = response.body().string();
            JsonNode jsonResponse = jsonMapper.readTree(responseBody);
            
            List<FileDto> files = new ArrayList<>();
            
            if (jsonResponse.isArray()) {
                for (JsonNode fileNode : jsonResponse) {
                    files.add(parseFileDto(fileNode));
                }
            } else if (jsonResponse.has("files") || jsonResponse.has("data")) {
                JsonNode filesArray = jsonResponse.has("files") ? 
                    jsonResponse.get("files") : jsonResponse.get("data");
                for (JsonNode fileNode : filesArray) {
                    files.add(parseFileDto(fileNode));
                }
            }
            
            return files;
        }
    }

    /**
     * Parse un nœud JSON en FileDto
     */
    private FileDto parseFileDto(JsonNode fileNode) {
        FileDto file = new FileDto();
        
        file.filename = fileNode.has("filename") ? fileNode.get("filename").asText() : 
                       fileNode.has("original_name") ? fileNode.get("original_name").asText() : "Sans nom";
        
        file.size = fileNode.has("size") ? fileNode.get("size").asLong() : 0L;
        
        file.folderId = fileNode.has("folder_id") && !fileNode.get("folder_id").isNull() ? 
            fileNode.get("folder_id").asInt() : null;
        
        // Parser la date
        file.uploadedAt = Instant.now();
        if (fileNode.has("uploaded_at")) {
            try {
                file.uploadedAt = Instant.parse(fileNode.get("uploaded_at").asText());
            } catch (Exception e) {
                // Garder la date actuelle en cas d'erreur
            }
        } else if (fileNode.has("created_at")) {
            try {
                file.uploadedAt = Instant.parse(fileNode.get("created_at").asText());
            } catch (Exception e) {
                // Garder la date actuelle en cas d'erreur
            }
        }
        
        // Version (à adapter selon votre API)
        file.currentVersion = fileNode.has("current_version") ? fileNode.get("current_version").asInt() : 
                             fileNode.has("version") ? fileNode.get("version").asInt() : 1;
        
        return file;
    }

    /**
     * Construit l'arborescence complète des dossiers avec leurs fichiers
     */
    public List<NodeItem> listRoot() {
        List<NodeItem> root = new ArrayList<>();
        
        try {
            // 1. Récupérer tous les dossiers
            List<FolderDto> allFolders = listFolders();
            
            // 2. Récupérer tous les fichiers
            List<FileDto> allFiles = listFiles();
            
            // 3. Créer une map pour accès rapide aux dossiers par ID
            Map<Integer, NodeItem> folderMap = new HashMap<>();
            
            // 4. Créer les NodeItem pour chaque dossier
            for (FolderDto folder : allFolders) {
                NodeItem node = NodeItem.folder(folder.name);
                node.setId(folder.id);
                node.setParentId(folder.parentId);
                folderMap.put(folder.id, node);
            }
            
            // 5. Construire la hiérarchie des dossiers
            List<NodeItem> rootFolders = new ArrayList<>();
            for (FolderDto folder : allFolders) {
                NodeItem node = folderMap.get(folder.id);
                
                if (folder.parentId == null) {
                    // Dossier racine
                    rootFolders.add(node);
                } else {
                    // Sous-dossier : ajouter au parent
                    NodeItem parent = folderMap.get(folder.parentId);
                    if (parent != null) {
                        parent.addChild(node);
                    } else {
                        // Parent introuvable, traiter comme racine
                        rootFolders.add(node);
                    }
                }
            }
            
            // 6. Ajouter les fichiers dans leurs dossiers respectifs
            for (FileDto file : allFiles) {
                FileEntry fileEntry = FileEntry.of(
                    file.filename, 
                    file.size, 
                    file.uploadedAt, 
                    file.currentVersion
                );
                
                if (file.folderId != null) {
                    NodeItem folder = folderMap.get(file.folderId);
                    if (folder != null) {
                        folder.getFiles().add(fileEntry);
                    }
                } else {
                    // Fichier sans dossier : créer un dossier "Non classé"
                    // ou l'ignorer selon votre logique métier
                }
            }
            
            // 7. Retourner les dossiers racine
            return rootFolders.isEmpty() ? createEmptyRoot() : rootFolders;
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la récupération de l'arborescence: " + e.getMessage());
            e.printStackTrace();
            return createEmptyRoot();
        }
    }
    
    /**
     * Crée un dossier racine vide en cas d'erreur
     */
    private List<NodeItem> createEmptyRoot() {
        List<NodeItem> root = new ArrayList<>();
        NodeItem emptyFolder = NodeItem.folder("Mes documents");
        root.add(emptyFolder);
        return root;
    }

    /**
     * Crée un nouveau dossier
     * POST /folders
     * @param name Nom du dossier
     * @param parentId ID du dossier parent (null pour la racine)
     * @return ID du dossier créé
     * @throws IOException En cas d'erreur réseau
     */
    public Integer createFolder(String name, Integer parentId) throws IOException {
        if (!isAuthenticated()) {
            throw new IOException("Non authentifié. Veuillez vous connecter d'abord.");
        }
        
        if (name == null || name.trim().isEmpty()) {
            throw new IOException("Le nom du dossier ne peut pas être vide");
        }
        
        // Créer le corps de la requête JSON
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("name", name.trim());
        if (parentId != null) {
            requestData.put("parent_id", parentId);
        }
        
        String jsonBody = jsonMapper.writeValueAsString(requestData);
        
        RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(baseUrl + "folders")
            .post(body)
            .addHeader("Authorization", "Bearer " + authToken)
            .addHeader("Content-Type", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                throw new IOException("Échec de la création du dossier: " + response.code() + " - " + responseBody);
            }
            
            // Parser la réponse pour récupérer l'ID du dossier
            if (!responseBody.isEmpty()) {
                try {
                    JsonNode jsonResponse = jsonMapper.readTree(responseBody);
                    
                    if (jsonResponse.has("id")) {
                        return jsonResponse.get("id").asInt();
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors du parsing de la réponse: " + e.getMessage());
                }
            }
            
            // Si pas d'ID retourné, considérer comme succès quand même
            return null;
        }
    }

    /**
     * Upload un fichier vers l'API
     * POST /files
     * @param file Fichier à uploader
     * @param folderId ID du dossier de destination (null pour la racine)
     * @return ID du fichier créé
     * @throws IOException En cas d'erreur réseau
     */
    public Integer uploadFile(java.io.File file, Integer folderId) throws IOException {
        if (!isAuthenticated()) {
            throw new IOException("Non authentifié. Veuillez vous connecter d'abord.");
        }
        
        if (file == null || !file.exists()) {
            throw new IOException("Fichier invalide ou inexistant");
        }
        
        // Créer le corps multipart/form-data
        okhttp3.MultipartBody.Builder builder = new okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("file", file.getName(),
                RequestBody.create(file, MediaType.parse("application/octet-stream")));
        
        // Ajouter le folder_id si spécifié
        if (folderId != null) {
            builder.addFormDataPart("folder_id", String.valueOf(folderId));
        }
        
        RequestBody requestBody = builder.build();
        
        Request request = new Request.Builder()
            .url(baseUrl + "files")
            .post(requestBody)
            .addHeader("Authorization", "Bearer " + authToken)
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Échec de l'upload: " + response.code() + " - " + errorBody);
            }
            
            // Parser la réponse pour récupérer l'ID du fichier
            String responseBody = response.body().string();
            JsonNode jsonResponse = jsonMapper.readTree(responseBody);
            
            if (jsonResponse.has("id")) {
                return jsonResponse.get("id").asInt();
            } else {
                // Si pas d'ID retourné, considérer comme succès quand même
                return null;
            }
        }
    }

    /**
     * Quota simulé: 2 Go max, 350 Mo utilisés.
     * TODO: Remplacer par un vrai appel API
     */
    public Quota getQuota() {
        return new Quota(350L * 1024 * 1024, 2L * 1024 * 1024 * 1024);
    }

    /**
     * DTO pour les dossiers
     */
    public static class FolderDto {
        public Integer id;
        public String name;
        public Integer parentId;
    }
    
    /**
     * DTO pour les fichiers
     */
    public static class FileDto {
        public String filename;
        public long size;
        public Integer folderId;
        public Instant uploadedAt;
        public int currentVersion;
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