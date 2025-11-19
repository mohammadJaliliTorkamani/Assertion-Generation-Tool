package org.example;

public class Accuracy {

    //these two fields will be used to calculate the total accuracy in outer output.json
    private int w;
    private double accuracy;

    public Accuracy() {

    }

    public Accuracy(int w, double accuracy) {
        this.w = w;
        this.accuracy = accuracy;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }
}
