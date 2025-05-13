public class Produto {
    int id;
    String nome;
    Float preco_inicial;
    Float preco_final;

    public Produto(int id, String nome, Float preco_inicial, Float preco_final) {
        this.id = id;
        this.nome = nome;
        this.preco_inicial = preco_inicial;
        this.preco_final = preco_final;
    }

    public Produto(int id, String nome, Float preco_inicial) {
        this.id = id;
        this.nome = nome;
        this.preco_inicial = preco_inicial;
        this.preco_final = 0f;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Float getPreco_inicial() {
        return preco_inicial;
    }

    public void setPreco_inicial(Float preco_inicial) {
        this.preco_inicial = preco_inicial;
    }

    public Float getPreco_final() {
        return preco_final;
    }

    public void setPreco_final(Float preco_final) {
        this.preco_final = preco_final;
    }


}
