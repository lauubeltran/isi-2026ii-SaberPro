import java.util.Map;

public class Estudiante {
    public static final String PENDIENTE = "PENDIENTE";
    public static final String HABILITADO = "HABILITADO";
    public static final String NO_HABILITADO = "NO_HABILITADO";

    private final String codigo;
    private final String nombre;
    private final String programa;
    private final int creditosAprobados;
    private final int creditosTotales;
    private final double promedio;
    private final Map<String, Boolean> requisitos;
    private String condicion;
    private String observaciones;

    public Estudiante(String codigo, String nombre, String programa, int creditosAprobados,
                      int creditosTotales, double promedio, Map<String, Boolean> requisitos,
                      String condicion, String observaciones) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.programa = programa;
        this.creditosAprobados = creditosAprobados;
        this.creditosTotales = creditosTotales;
        this.promedio = promedio;
        this.requisitos = requisitos;
        this.condicion = condicion;
        this.observaciones = observaciones == null ? "" : observaciones;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getPrograma() { return programa; }
    public int getCreditosAprobados() { return creditosAprobados; }
    public int getCreditosTotales() { return creditosTotales; }
    public double getPromedio() { return promedio; }
    public Map<String, Boolean> getRequisitos() { return requisitos; }
    public String getCondicion() { return condicion; }
    public void setCondicion(String condicion) { this.condicion = condicion; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String o) { this.observaciones = o == null ? "" : o; }

    public double getPorcentajeAvance() {
        return creditosTotales == 0 ? 0 : 100.0 * creditosAprobados / creditosTotales;
    }

    public boolean estaEvaluado() {
        return !PENDIENTE.equals(condicion);
    }
}