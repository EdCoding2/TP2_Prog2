module ca.cegep.biblio {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens ca.cegep.biblio.persistence to com.google.gson;
    opens ca.cegep.biblio.model to com.google.gson;
    opens ca.cegep.biblio.gui.controller to javafx.fxml;
    opens ca.cegep.biblio.gui to javafx.fxml;

    exports ca.cegep.biblio.gui;
}
