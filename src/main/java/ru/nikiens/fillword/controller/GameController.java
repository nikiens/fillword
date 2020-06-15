package ru.nikiens.fillword.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXListView;
import javafx.collections.*;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;

import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import ru.nikiens.fillword.model.CellState;
import ru.nikiens.fillword.model.Game;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class GameController implements Initializable {

    @FXML
    private GridPane table;

    @FXML
    private Label category;

    @FXML
    private JFXListView<String> wordsList;

    @FXML
    private StackPane stackPane;

    private PseudoClass selected = PseudoClass.getPseudoClass("selected");
    private PseudoClass marked = PseudoClass.getPseudoClass("marked");

    private ObservableSet<Label> selectedCells = FXCollections.observableSet(new LinkedHashSet<>());
    private Set<String> words = Game.getInstance().getWords();

    private final int BOARD_SIZE = Game.getInstance().getBoardSize().value();

    public void initialize(URL location, ResourceBundle resources) {
        GridLocation dragged = new GridLocation();

        Game.getInstance().initializeBoard();
        Game.getInstance().fillWithWords();
        Game.getInstance().fillWithLetters();

        wordsList.setItems(FXCollections.observableArrayList(Game.getInstance().getWords()));
        wordsList.setCellFactory(lc -> createWordCell());

        category.setText(Game.getInstance().getCategory());

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                table.add(generateLabel(i, j, dragged), i, j);
            }
        }

        selectedCells.addListener((SetChangeListener.Change<? extends Label> change) -> {
            if (change.wasAdded()) {
                Label label = change.getElementAdded();
                label.pseudoClassStateChanged(selected, true);
            } else if (change.wasRemoved()) {
                Label label = change.getElementRemoved();
                label.pseudoClassStateChanged(selected, false);
            }
        });
    }

    private ListCell<String> createWordCell() {
        return new ListCell<>() {
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null) {
                    setText(item);
                    setId("wordCell-" + item);
                } else {
                    setText(null);
                }
            }
        };
    }

    private Label generateLabel(int x, int y, GridLocation gridLocation) {
        Label label = new Label(Character.toString(Game.getInstance().getCell(x, y).getLetter()));

        label.prefHeightProperty().bind(table.heightProperty().divide(BOARD_SIZE));
        label.prefWidthProperty().bind(table.widthProperty().divide(BOARD_SIZE));
        label.setAlignment(Pos.CENTER);

        label.setOnDragDetected(event -> {
            gridLocation.x = x;
            gridLocation.y = y;

            selectedCells.clear();
            selectedCells.add(label);
            label.startFullDrag();
        });

        label.setOnMouseDragEntered(event -> recomputeSelection(gridLocation, x, y));
        label.setOnMouseDragReleased(event -> processSelection());

        return label;
    }

    private void recomputeSelection(GridLocation dragged, int x, int y) {
        Set<Label> selection = new HashSet<>();
        Set<Label> horizontalSelection = new HashSet<>();
        Set<Label> diagonalSelection = new HashSet<>();

        for (int j = dragged.y; j <= y; j++) {
            for (int i = dragged.x; i <= x; i++) {
                Label label = (Label) table.getChildren().get(i * BOARD_SIZE + 1 + j);

                if (Game.getInstance().getCell(j, i).getState() == CellState.MARKED) {
                    return;
                }

                if (j == dragged.y) {
                    selection.add(label);
                }
                if (i == dragged.x) {
                    horizontalSelection.add(label);
                    selection.clear();
                    selection.addAll(horizontalSelection);
                }
                if (j - i == dragged.y - dragged.x) {
                    diagonalSelection.add(label);
                    selection.clear();
                    selection.addAll(diagonalSelection);
                }
            }
        }
        selectedCells.retainAll(selection);
        selectedCells.addAll(selection);
    }

    private void processSelection() {
        String word = selectedCells.stream().map(Labeled::getText).collect(Collectors.joining());

        if (words.contains(word)) {
            words.remove(word);
            wordsList.lookup("#wordCell-" + word).pseudoClassStateChanged(selected, true);

            selectedCells.forEach(it -> {
                int x = GridPane.getRowIndex(it);
                int y = GridPane.getColumnIndex(it);

                Game.getInstance().getCell(x, y).setState(CellState.MARKED);
                it.pseudoClassStateChanged(marked, true);
            });
        }

        selectedCells.clear();

        if (words.isEmpty()) {
            finishGame();
        }
    }

    private void finishGame() {
        stackPane.setVisible(true);

        JFXDialogLayout content = new JFXDialogLayout();
        content.setHeading(new Text("You won!"));
        content.setBody(new Text("You have successfully completed the level!"));

        JFXButton button = new JFXButton("OK");
        JFXDialog dialog = new JFXDialog(stackPane, content, JFXDialog.DialogTransition.CENTER);

        button.setOnAction(event -> dialog.close());
        button.setButtonType(JFXButton.ButtonType.RAISED);

        content.setActions(button);

        dialog.setId("endingDialog");
        dialog.show();
    }

    private static class GridLocation {
        int x, y;
    }
}