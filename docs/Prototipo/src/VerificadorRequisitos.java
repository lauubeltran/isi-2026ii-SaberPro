import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VerificadorRequisitos {
    public static final double AVANCE_MINIMO = 75.0;   // % de créditos aprobados (ajústalo)
    public static final double PROMEDIO_MINIMO = 3.0;  // (ajústalo)

    public static class Item {
        public final String nombre;
        public final String detalle;
        public final boolean cumple;

        public Item(String nombre, String detalle, boolean cumple) {
            this.nombre = nombre;
            this.detalle = detalle;
            this.cumple = cumple;
        }
    }

    public static class Resultado {
        public final List<Item> items = new ArrayList<>();

        public int pendientes() {
            int n = 0;
            for (Item i : items) if (!i.cumple) n++;
            return n;
        }

        public boolean habilitado() {
            return pendientes() == 0;
        }
    }

    public Resultado verificar(Estudiante e) {
        Resultado r = new Resultado();

        double avance = e.getPorcentajeAvance();
        r.items.add(new Item("Avance del plan",
                String.format("%d / %d créditos (%.1f%%), mínimo %.0f%%",
                        e.getCreditosAprobados(), e.getCreditosTotales(), avance, AVANCE_MINIMO),
                avance >= AVANCE_MINIMO));

        r.items.add(new Item("Promedio académico",
                String.format("%.1f, mínimo %.1f", e.getPromedio(), PROMEDIO_MINIMO),
                e.getPromedio() >= PROMEDIO_MINIMO));

        for (Map.Entry<String, Boolean> en : e.getRequisitos().entrySet()) {
            boolean ok = en.getValue();
            r.items.add(new Item(formatear(en.getKey()), ok ? "Cumple" : "Pendiente", ok));
        }
        return r;
    }

    private String formatear(String clave) {
        String s = clave.replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}