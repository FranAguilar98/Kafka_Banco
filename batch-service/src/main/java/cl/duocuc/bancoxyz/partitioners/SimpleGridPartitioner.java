package cl.duocuc.bancoxyz.partitioners;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class SimpleGridPartitioner implements Partitioner {

    @Value("${app.batch.input.cuentas-anuales}")
    private Resource cuentasAnualesResource;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> particiones = new HashMap<>();

        int totalLineas = contarLineas(cuentasAnualesResource);
        int totalDatos = Math.max(totalLineas - 1, 0); 

        int base = totalDatos / gridSize;
        int resto = totalDatos % gridSize;

        int lineasLeidasHastaAhora = 0;

        for (int i = 0; i < gridSize; i++) {
            int itemsParaEstaParticion = base + (i < resto ? 1 : 0);

            int linesToSkip = 1 + lineasLeidasHastaAhora;

            ExecutionContext contexto = new ExecutionContext();
            contexto.putInt("partitionIndex", i);
            contexto.putInt("gridSize", gridSize);
            contexto.putInt("linesToSkip", linesToSkip);
            contexto.putInt("maxItemCount", itemsParaEstaParticion);

            particiones.put("partition" + i, contexto);

            lineasLeidasHastaAhora += itemsParaEstaParticion;
        }

        return particiones;
    }

    private int contarLineas(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            int lineas = 0;
            while (reader.readLine() != null) {
                lineas++;
            }
            return lineas;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo contar las lineas del archivo de cuentas anuales", e);
        }
    }
}