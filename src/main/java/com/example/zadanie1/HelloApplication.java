package com.example.zadanie1;


import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

import java.awt.image.WritableRaster;
import java.io.*;
import java.util.Arrays;

public class HelloApplication extends Application {

    public static int histogramRed[] = new int[256];
    public static int histogramGreen[] = new int[256];
    public static int histogramBlue[] = new int[256];
    public static int histogramAvg[] = new int[256];

    String imagePath = System.getProperty("user.dir") + "\\img\\default.jpg";
    ImageView originalImageView = new ImageView();
    ImageView convertedImageView = new ImageView();
    ImageView equalizedImgImageView = new ImageView();
    File file = new File(imagePath);

    Image originalImage;
    Image convertedImage;

    BufferedImage img;

    VBox ImageBox = new VBox();
    VBox HistogramBox = new VBox();
    @Override
    public void start(Stage stage) throws IOException {

        // Get Orginal Image and convert

        InputStream stream = new FileInputStream(imagePath);
        img = ImageIO.read(file);
        originalImage = convertToFxImage(img);

        BufferedImage binaryImage = convertToBinarization(img);
        convertedImage = convertToFxImage(binaryImage);

        originalImageView.setImage(originalImage);
        convertedImageView.setImage(convertedImage);


        // Loading Image
        Button loadBtn = new Button("Load");
        loadBtn.setMinWidth(100);
        loadBtn.setOnAction(e -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png"));

                    file = fileChooser.showOpenDialog(stage);

                    try {
                        start(stage);
                    }
                    catch (IOException ex){
                        ex.printStackTrace();
                    }

                }
        );

        // Saving Image
        Button saveBtn = new Button("save");
        saveBtn.setMinWidth(100);
        saveBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName("out.jpg");

            File outDir = new File(System.getProperty("user.home"));
            fileChooser.setInitialDirectory(outDir);

            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG", "jpg"));

            File out = fileChooser.showSaveDialog(stage);
            try {
                ImageIO.write(img, "jpg", out);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }

        });

        // Equalize Histogram
        Button equalizeHistogramBtn = new Button("equalize Histogram");
        equalizeHistogramBtn.setMinWidth(200);
        equalizeHistogramBtn.setOnAction(e -> {

            //Check is image is loaded
            if (histogramRed == null)
            {
                return;
            }

            BufferedImage grayScaleImg = getGrayscaleImage(img);

            BufferedImage equalizedImg = equalize(grayScaleImg);

            equalizedImgImageView.setImage(convertToFxImage(equalizedImg));

            //Equalized Image display
            equalizedImgImageView.setX(10);
            equalizedImgImageView.setY(700);
            equalizedImgImageView.setFitHeight(300);
            equalizedImgImageView.setFitWidth(300);
            equalizedImgImageView.setPreserveRatio(true);


            ImageBox.getChildren().addAll(equalizedImgImageView);


        });

        HBox buttonBox = new HBox();
        buttonBox.getChildren().addAll(loadBtn, saveBtn, equalizeHistogramBtn);



        //Original Image display
        originalImageView.setX(10);
        originalImageView.setY(10);
        originalImageView.setFitHeight(300);
        originalImageView.setFitWidth(300);
        originalImageView.setPreserveRatio(true);

        //Converted Image display
        convertedImageView.setX(10);
        convertedImageView.setY(380);
        convertedImageView.setFitHeight(300);
        convertedImageView.setFitWidth(300);
        convertedImageView.setPreserveRatio(true);




        ImageBox.getChildren().addAll(originalImageView, convertedImageView);


        // Histogram display
        // Defining Axis
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        LineChart<Number, Number> histogramChart = new LineChart<>(xAxis, yAxis);
        histogramChart.setCreateSymbols(false);


        // Defining Series
        XYChart.Series seriesRed = new XYChart.Series<>();
        XYChart.Series seriesGreen = new XYChart.Series<>();
        XYChart.Series seriesBlue= new XYChart.Series<>();
        XYChart.Series seriesAvg = new XYChart.Series<>();

        // Setting Series Labels
        seriesRed.setName("red");
        seriesGreen.setName("green");
        seriesBlue.setName("blue");
        seriesAvg.setName("average");

        // Setting Series Data
        for(int i = 0; i < 256; i++) {
            seriesRed.getData().add(new XYChart.Data(i, histogramRed[i]));
            seriesGreen.getData().add(new XYChart.Data(i, histogramGreen[i]));
            seriesBlue.getData().add(new XYChart.Data(i, histogramBlue[i]));
            seriesAvg.getData().add(new XYChart.Data(i, histogramAvg[i]));
        }

        histogramChart.getData().addAll(seriesRed, seriesAvg, seriesGreen, seriesBlue );

        HistogramBox.getChildren().addAll(histogramChart);


        HBox hBox = new HBox();
        hBox.getChildren().addAll(ImageBox, HistogramBox, buttonBox);


        /***** Otsu Binarization  *****/
        // Byte array
        BufferedImage grayscaleOriginalImg = getGrayscaleImage(img);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(grayscaleOriginalImg, "jpg", bos);
        byte [] srcData = bos.toByteArray();

        int[] histData = new int[256];

        // Calculate histogram
        int ptr = 0;
        while (ptr < srcData.length) {
            int h = 0xFF & srcData[ptr];
            histData[h] ++;
            ptr ++;
        }

// Total number of pixels
        int total = srcData.length;

        float sum = 0;
        for (int t=0 ; t<256 ; t++) sum += t * histData[t];

        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;
        int threshold = 120;

        for (int t=0 ; t<256 ; t++) {
            wB += histData[t];               // Weight Background
            if (wB == 0) continue;

            wF = total - wB;                 // Weight Foreground
            if (wF == 0) break;

            sumB += (float) (t * histData[t]);

            float mB = sumB / wB;            // Mean Background
            float mF = (sum - sumB) / wF;    // Mean Foreground

            // Calculate Between Class Variance
            float varBetween = (float)wB * (float)wF * (mB - mF) * (mB - mF);

            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }


        Group root = new Group(hBox);
        Scene scene = new Scene(root, 1295, 800);

        stage.setTitle("Binarization And Histogram");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }


    private static BufferedImage convertToBinarization(BufferedImage image) {
        // Reset Histogram
        Arrays.fill(histogramRed, 0);
        Arrays.fill(histogramGreen, 0);
        Arrays.fill(histogramBlue, 0);
        Arrays.fill(histogramAvg, 0);

        // Get the width of the image.
        int width = image.getWidth();
        // Get the height of the image.
        int height = image.getHeight();
        // Establish a 2D array to keep image data.
        int[][] result = new int[width][height];
        // The height of the image.
        for (int row = 0; row < width; row++) {
            // The width of the image.
            for (int col = 0; col < height; col++) {
                // Acquire RGB value.
                result[row][col] = image.getRGB(row, col);
                // Acqurie integer type of RGB value.
                int iRet = result[row][col];
                // The red variable.
                int iR = 0;
                // The green variable.
                int iG = 0;
                // The bule variable.
                int iB = 0;
                // The average variable
                int iAvg = 0;




                // Get blue pixel of the blue channel.
                iB = ((int) iRet & 0xff);
                histogramBlue[iB]++;
                // Get green pixel of the green channel.
                iG = (((int) iRet & 0x00ff00) >> 8);
                histogramGreen[iG]++;
                // Get red pixel of the red channel.
                iR = (((int) iRet & 0xff0000) >> 16);
                histogramRed[iR]++;
                // Transform RGB color space to gray scale.
                iAvg = ( iR + iG + iB ) / 3;
                histogramAvg[iAvg]++;
                // This is our threshold, you can change it.
                if ( iAvg > 120 )
                {
                    // This is white pixel.
                    iRet = 0xffffff;
                    // Set the white pixel into the image.
                    image.setRGB(row, col, iRet);
                }
                else
                {
                    // Set the black pixel into the image.
                    image.setRGB(row, col, 0);
                }
            }
        }
        // return result value.
        return image;
    }

    BufferedImage getGrayscaleImage(BufferedImage src) {
        BufferedImage gImg = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster wr = src.getRaster();
        WritableRaster gr = gImg.getRaster();
        for(int i=0;i<wr.getWidth();i++){
            for(int j=0;j<wr.getHeight();j++){
                gr.setSample(i, j, 0, wr.getSample(i, j, 0));
            }
        }
        gImg.setData(gr);
        return gImg;
    }

    BufferedImage equalize(BufferedImage src){
        BufferedImage nImg = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        WritableRaster wr = src.getRaster();
        WritableRaster er = nImg.getRaster();

        int totpix= wr.getWidth() * wr.getHeight();
        int[] histogram = new int[256];

        for (int x = 0; x < wr.getWidth(); x++) {
            for (int y = 0; y < wr.getHeight(); y++) {
                histogram[wr.getSample(x, y, 0)]++;
            }
        }

        int[] chistogram = new int[256];
        chistogram[0] = histogram[0];
        for(int i=1;i<256;i++){
            chistogram[i] = chistogram[i-1] + histogram[i];
        }

        float[] arr = new float[256];
        for(int i=0;i<256;i++){
            arr[i] =  (float)((chistogram[i]*255.0)/(float)totpix);
        }

        for (int x = 0; x < wr.getWidth(); x++) {
            for (int y = 0; y < wr.getHeight(); y++) {
                int nVal = (int) arr[wr.getSample(x, y, 0)];
                er.setSample(x, y, 0, nVal);
            }
        }
        nImg.setData(er);

        // Histogram display
        // Defining Axis
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        LineChart<Number, Number> histogramEqualizeChart = new LineChart<>(xAxis, yAxis);
        histogramEqualizeChart.setCreateSymbols(false);


        // Defining Series
        XYChart.Series serieEqualize = new XYChart.Series<>();


        // Setting Series Labels
        serieEqualize.setName("serie");

        // Setting Series Data
        for(int i = 0; i < 255; i++) {
            serieEqualize.getData().add(new XYChart.Data(i, chistogram[i]));
        }

        histogramEqualizeChart.getData().addAll(serieEqualize);

        HistogramBox.getChildren().removeAll();
        HistogramBox.getChildren().addAll(histogramEqualizeChart);

        return nImg;
    }

    private static Image convertToFxImage(BufferedImage image) {
        WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pw.setArgb(x, y, image.getRGB(x, y));
                }
            }
        }

        return new ImageView(wr).getImage();
    }

    private int [] calculateLUT(int[] values, int size) {

        double minValue = 0;
        for (int i = 0; i < 256; i++)
        {
            if (values[i] != 0)
            {
                minValue = values[i];
                break;
            }
        }

        //przygotuj tablice zgodnie ze wzorem
        int[] result = new int[256];
        double sum = 0;
        for (int i = 0; i < 256; i++)
        {
            sum += values[i];
            result[i] = (int)(((sum - minValue) / (size - minValue)) * 255.0);
        }

        return result;
    }

    private void equalize() {

    }
}




