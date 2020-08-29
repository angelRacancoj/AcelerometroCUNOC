package com.usac.cunoc.acelerografo2.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 *
 * @author angelrg
 */
public class ManejadorArchivo {

    public void guardarArchivo(String path) throws IOException {
        System.out.println("Path: " + path);
        FileWriter fichero = null;
        File file = new File(path);
        fichero = new FileWriter(file);
        fichero.write("'Fecha y Hora','Aceleracion X','Aceleracion Y','AceleracionZ'\n");
        fichero.close();
    }

    public void addRow(LocalDateTime time, Double accX, Double accY, Double accZ, String path) {
        String row = "'" + time.toString() + "'," + accX + "," + accY + "," + accZ;
        appendToFile(path, row);
    }

    private void appendToFile(String path, String texto) {
        File file = new File(path);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.append((texto + "\n"));
            writer.close();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

}
