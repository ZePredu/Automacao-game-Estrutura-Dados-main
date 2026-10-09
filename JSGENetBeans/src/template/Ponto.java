package template;

import java.util.Objects;

/**
 * Classe simples pra guardar a coordenada (coluna, linha) no grid do mapa.
 * 
 * A gente criou ela pra usar como chave no HashMap (mapaEsteiras e ocupacao).
 * Como é um objeto customizado, tivemos que dar override no equals() e no hashCode() 
 * pro Java conseguir achar a posição certa comparando os valores de col e row, 
 * e não o endereço de memória do objeto.
 */

public class Ponto {
    private int col;
    private int row;

    public Ponto(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getCol() { return col; }
    public int getRow() { return row; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ponto ponto = (Ponto) o;
        return col == ponto.col && row == ponto.row;
    }

    @Override
    public int hashCode() {
        return Objects.hash(col, row);
    }
}