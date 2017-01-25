import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;

import java.io.File;
import java.io.IOException;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import javax.imageio.ImageIO;

import javax.swing.JFrame;
import javax.swing.JPanel;

class Evolve extends JFrame{
	
	public Random random = new Random();

	public Circle[][] population;
	public double[] fitness;

	public BufferedImage image;
	public JPanel panel;
	public int width, height, generation;

	public final int MIN_RADIUS = 5;
	public final int MAX_RADIUS = 10;
	public final int GENE_SIZE = 600;
	public final int POPULATION_SIZE = 75;
	public final int TOURNAMENT_SIZE = 15;
	public final int ELITE_SIZE = 10;

	public final double MUTATION_RATE = 0.02;
	public final double MUTATION_AMOUNT = 0.01;

	public static void main(String[] args) {
		new Evolver();
	}

	public Evolver(){

		generation = 0;

		fitness = new double[POPULATION_SIZE];
		image = loadImage("darwin");
		initPopulation();
		panel = initPanel();

		this.setTitle("Evolve");
		this.setSize(width*2,height);
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
		this.add(panel);

		do{
			population = nextGeneration(population);
			double[] f = fitness;
			Arrays.sort(f);
			System.out.println("Generation "+generation+": "+f[0]+", "+f[f.length-1]);
			generation++;
			panel.repaint();
		}while(1d-fitness[0] < 0.95);
	}

	public Circle[][] nextGeneration(Circle[][] circles){
		Circle[][] res = new Circle[POPULATION_SIZE][GENE_SIZE];
		fitness = evaluate(circles);
		//POSSIBLY IMPLEMENT KEEPING THE TOP PERCENT

		Circle[] t1 = getTournamentWinner(circles);
		Circle[] t2 = getTournamentWinner(circles);

		for (int i = 0; i < POPULATION_SIZE; i++) {
			if(!(i%ELITE_SIZE==0)){
				t1 = getTournamentWinner(circles);
				t2 = getTournamentWinner(circles);
			}
			res[i] = crossover(t1,t2);
			if(random.nextDouble() < MUTATION_RATE){
				//int change = (int)Math.floor(MUTATION_AMOUNT*GENE_SIZE);
				//for(int j = 0; j < change; j++){
					res[i][random.nextInt(GENE_SIZE)] = initRandomCircle();
				//}
			}
		}

		return res;
	}

	public Circle[] getTournamentWinner(Circle[][] circles){

		int winner 	= -1;
		double maxF = 1d;
		
		for(int i = 0; i < TOURNAMENT_SIZE; i++){
			int index	= random.nextInt(POPULATION_SIZE);
			double f 	= fitness[index];
			if(f < maxF){
				maxF 	= f;
				winner 	= index;
			}
		}
		return circles[winner];
	}

	public Circle[] crossover(Circle[] c1, Circle[] c2){
		int crossOverPoint = random.nextInt(GENE_SIZE-2)+1;

		Circle[] child = new Circle[GENE_SIZE];
		
		for(int i = 0; i < crossOverPoint; i++){
			child[i] = c1[i];
		}

		for(int i = crossOverPoint; i < GENE_SIZE; i++){
			child[i] = c2[i];
		}

		return child;
	}

	public double[] evaluate(Circle[][] img){
		double[] f = new double[POPULATION_SIZE];
		for (int i = 0; i < POPULATION_SIZE; i++) {
			f[i] = fitness(image, genesToPhenotype(img[i]));
		}
		return f;
	}

	public double fitness(BufferedImage i1, BufferedImage i2){
		int width 	= i1.getWidth(null);
		int height 	= i1.getHeight(null);
		long diff = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb1 = i1.getRGB(x, y);
				int rgb2 = i2.getRGB(x, y);
				int r1 = (rgb1 >> 16) & 0xff;
				int g1 = (rgb1 >>  8) & 0xff;
				int b1 = (rgb1      ) & 0xff;
				int r2 = (rgb2 >> 16) & 0xff;
				int g2 = (rgb2 >>  8) & 0xff;
				int b2 = (rgb2      ) & 0xff;
				diff += Math.abs(r1 - r2);
				diff += Math.abs(g1 - g2);
				diff += Math.abs(b1 - b2);
			}
		}
		double n = width * height * 3;
		double p = diff / n / 255.0;
		//System.out.println("diff percent: " + (p * 100.0));
		return p;
	}

	/*
	 Initialises the population but putting random circles for each individual
	*/
	public void initPopulation(){
		population 	= new Circle[POPULATION_SIZE][GENE_SIZE];

		for (int i = 0; i < POPULATION_SIZE; i++) {
			for (int j = 0; j < GENE_SIZE; j++) {
				population[i][j] = initRandomCircle();
			}
		}
	}

	public JPanel initPanel(){
		return new JPanel(){
			@Override
			public void paintComponent(Graphics g){
				super.paintComponent(g);
				g.drawImage(image,0,0,null);
				g.drawImage(genesToPhenotype(population[findSmallest(fitness)]),width,0,null);
			}
		};
	}

	public Circle initRandomCircle(){
		int col = random.nextInt(255)+1;
		return new Circle(random.nextInt(width)+1, random.nextInt(height)+1, random.nextInt(MAX_RADIUS-MIN_RADIUS)+1+MIN_RADIUS, new Color(col,col,col,255));
	}

	public BufferedImage loadImage(String s){
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File(s+".jpg"));
		    width = img.getWidth();
		    height = img.getHeight();
		} catch (IOException e) {
		}
		return img;
	}

	public BufferedImage genesToPhenotype(Circle[] circles){
		BufferedImage bimage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bimage.createGraphics();

		for (Circle c : circles) {
			g2d.setColor(c.c);
			g2d.fill(new Ellipse2D.Double(c.x-c.r, c.y-c.r, c.r*2, c.r*2));		
		}

		return bimage;
	}

	public void printCircles(Circle[] circles){
		for (Circle c : circles) {
			System.out.println(c.r);
		}
	}

	public void printPop(){
		for (Circle[] c : population) {
			System.out.println(c);
		}
	}

	public void printFitness(){
		for (double d : fitness) {
			System.out.println(d);
		}
	}

	public static int findSmallest (double[] arr1){//start method

       int index = 0;
       double min = arr1[index];
       for (int i=1; i<arr1.length; i++){
           if (arr1[i] < min ){
               min = arr1[i];
               index = i;
           }
       }
       return index ;

}
}



class Circle{

	public int x;
	public int y;
	public int r;

	public Color c;

	public Circle(int x, int y, int r, Color c){
		this.x = x;
		this.y = y;
		this.r = r;

		this.c = c;
	}

}