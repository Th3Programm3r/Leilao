public class LanceDTO {
    String produto;
    int leilao;
    float valor;

    public LanceDTO(String produto, int leilao, float valor) {
        this.produto = produto;
        this.leilao = leilao;
        this.valor = valor;
    }

    public String getProduto() {
        return produto;
    }

    public void setProduto(String produto) {
        this.produto = produto;
    }

    public int getLeilao() {
        return leilao;
    }

    public void setLeilao(int leilao) {
        this.leilao = leilao;
    }

    public float getValor() {
        return valor;
    }

    public void setValor(float valor) {
        this.valor = valor;
    }
}
