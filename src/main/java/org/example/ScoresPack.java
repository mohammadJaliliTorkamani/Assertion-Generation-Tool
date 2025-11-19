package org.example;

public class ScoresPack {
    private RougeLAverageScore rougeLAverageScore;
    private JaccardAverageScore jaccardAverageScore;

    public ScoresPack() {
    }

    public ScoresPack(RougeLAverageScore rougeLAverageScore, JaccardAverageScore jaccardAverageScore) {
        this.rougeLAverageScore = rougeLAverageScore;
        this.jaccardAverageScore = jaccardAverageScore;
    }

    public JaccardAverageScore getJaccardAverageScore() {
        return jaccardAverageScore;
    }

    public void setJaccardAverageScore(JaccardAverageScore jaccardAverageScore) {
        this.jaccardAverageScore = jaccardAverageScore;
    }

    public RougeLAverageScore getRougeLAverageScore() {
        return rougeLAverageScore;
    }

    public void setRougeLAverageScore(RougeLAverageScore rougeLAverageScore) {
        this.rougeLAverageScore = rougeLAverageScore;
    }
}
