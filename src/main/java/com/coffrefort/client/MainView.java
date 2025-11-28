package com.coffrefort.client;

import java.io.File;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

/**
 * Fenêtre principale simplifiée: à gauche l'arborescence, à droite la liste de fichiers,
 * en bas une barre de quota et des actions. Tout est simulé mais prêt pour des appels REST.
 */
public class MainView {
    private final ApiClient apiClient;

    private final BorderPane root = new BorderPane();
    private final TreeView<NodeItem> treeView = new TreeView<>();
    private final TableView<FileEntry> table = new TableView<>();
    private final ProgressBar quotaBar = new ProgressBar(0);
    private final Label quotaLabel = new Label();

    public MainView(ApiClient apiClient) {
        this.apiClient = apiClient;
        buildUi();
        loadData();
    }

    private void buildUi() {
        root.setPadding(new Insets(10));

        // Barre d'actions + quota
        Button NewfileBtn = new Button("Uploader (simulation)");
        NewfileBtn.setOnAction(e -> simulateUpload());

        HBox top = new HBox(NewfileBtn);
        root.setTop(top);

        // Arborescence
        treeView.setShowRoot(false);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) refreshFiles(sel.getValue());
        });
        root.setLeft(treeView);

        // Tableau de fichiers
        TableColumn<FileEntry, String> nameCol = new TableColumn<>("Nom");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(260);

        TableColumn<FileEntry, Long> sizeCol = new TableColumn<>("Taille");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
        sizeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Long size, boolean empty) {
                super.updateItem(size, empty);
                setText(empty || size == null ? null : humanSize(size));
            }
        });

        TableColumn<FileEntry, String> dateCol = new TableColumn<>("Modifié le");
        dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getUpdatedAt().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        ));

        table.getColumns().addAll(nameCol, sizeCol, dateCol);
        root.setCenter(table);

        // Barre d'actions + quota
        Button uploadBtn = new Button("Uploader (simulation)");
        uploadBtn.setOnAction(e -> simulateUpload());

        HBox bottom = new HBox(10, uploadBtn, new Label("Quota:"), quotaBar, quotaLabel);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        quotaBar.setPrefWidth(300);
        root.setBottom(bottom);
        
    }

    private void loadData() {
        // Arbre
        List<NodeItem> roots = apiClient.listRoot();
        TreeItem<NodeItem> hiddenRoot = new TreeItem<>(NodeItem.folder("root"));
        for (NodeItem n : roots) {
            hiddenRoot.getChildren().add(buildTree(n));
        }
        treeView.setRoot(hiddenRoot);
        if (!hiddenRoot.getChildren().isEmpty()) {
            treeView.getSelectionModel().select(hiddenRoot.getChildren().get(0));
        }

        // Quota
        Quota q = apiClient.getQuota();
        quotaBar.setProgress(q.getUsageRatio());
        quotaLabel.setText(humanSize(q.getUsed()) + " / " + humanSize(q.getMax()));
    }

    private TreeItem<NodeItem> buildTree(NodeItem node) {
        TreeItem<NodeItem> ti = new TreeItem<>(node);
        for (NodeItem child : node.getChildren()) {
            ti.getChildren().add(buildTree(child));
        }
        ti.setExpanded(true);
        return ti;
    }

    private void refreshFiles(NodeItem node) {
        table.getItems().setAll(node.getFiles());
    }

    private void simulateUpload() {
        // Juste une sélection de fichier locale et un message pour montrer le flux
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier à envoyer (simulation)");
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText("Upload simulé");
            ok.setContentText("Fichier sélectionné: " + file.getName());
            ok.showAndWait();
        }
    }

    public Node getRoot() { return root; }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        double v = bytes;
        String[] units = {"Ko", "Mo", "Go", "To"};
        int i = -1;
        while (v >= 1024 && i < units.length - 1) { v /= 1024.0; i++; }
        return new DecimalFormat("0.##").format(v) + " " + units[i];
    }
}
