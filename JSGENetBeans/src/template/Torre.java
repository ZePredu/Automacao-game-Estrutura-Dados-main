package template;

/** os arquivos "Inimigo.java" e "Torre.java" são arquivos da ideia antiga do nosso projeto
* inicialmente iamos fazer um Tower Defense com uma horda de inimigos
* mas tava ficando bem feio e bem chato na real, foi aí que percebi que os as bolinhas
* passando na tela se pareciam muito com os itens passando nas esteiras de joguinhos de
* automação como Factorio, Mindustry, etc
* Resolvi deixar os arquivos aqui caso eu quisesse implementar uma especíe de jogo de defesa
* em que vc tem q automatizar a produção e se defender de uma horda de inimigos
*  */


public class Torre {
    
    // Os tipos batem exatamente com as descrições da loja na Main
    public enum Tipo {
        ARQUEIRA("Arqueira", 50, 0.0, 0, 180, 20, 0.6),
        CANHAO("Canhão AoE", 100, 70.0, 2, 150, 45, 1.5),
        GELO("Torre Lenta", 80, 0.0, 3, 130, 10, 1.0);

        public final String nome;
        public final int custo;
        public final double raioExplosao;
        public final int cor;
        public final double alcance;
        public final int danoBase;
        public final double cooldownBase;

        Tipo(String nome, int custo, double raioExplosao, int cor, double alcance, int danoBase, double cooldownBase) {
            this.nome = nome;
            this.custo = custo;
            this.raioExplosao = raioExplosao;
            this.cor = cor;
            this.alcance = alcance;
            this.danoBase = danoBase;
            this.cooldownBase = cooldownBase;
        }
    }

    private Tipo tipo;
    private double x, y;
    private int nivel = 1;
    private double timerAtaque = 0;

    public Torre(Tipo tipo, double x, double y) {
        this.tipo = tipo;
        this.x = x;
        this.y = y;
    }

    public void atualizar(double delta) {
        if (timerAtaque > 0) timerAtaque -= delta;
    }

    public boolean pronta() { 
        return timerAtaque <= 0; 
    }
    
    public void resetar() { 
        timerAtaque = Math.max(0.2, tipo.cooldownBase - (nivel - 1) * 0.15); 
    }
    
    public void evoluir() { 
        if (nivel < 3) nivel++; 
    }

    public int getDano() { return tipo.danoBase + (nivel - 1) * (tipo.danoBase / 2); }
    public double getAlcance() { return tipo.alcance + (nivel - 1) * 20; }
    public double getLentidao() { return tipo == Tipo.GELO ? (0.4 + nivel * 0.1) : 0.0; } // Aplica slow progressivo

    public int getCustoUpgrade() { return tipo.custo * nivel; }
    public int getValorVenda() { return (tipo.custo + (nivel > 1 ? getCustoUpgrade() / 2 : 0)) / 2; }

    public double getX() { return x; }
    public double getY() { return y; }
    public Tipo getTipo() { return tipo; }
    public int getNivel() { return nivel; }
}