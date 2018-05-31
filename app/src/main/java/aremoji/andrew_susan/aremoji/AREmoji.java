package aremoji.andrew_susan.aremoji;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
/*
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
*/
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class AREmoji extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 777;
    private static final int MAX_LABEL_RESULTS = 4;

    ImageView cameraImage;
    TextView labels;
    Feature feature;
    ArrayList<Feature> listOfFeatures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aremoji);

        //create and set feature to detect in image while limiting number of results
        feature = new Feature();
        feature.setType("LABEL_DETECTION");
        feature.setMaxResults(MAX_LABEL_RESULTS);

        //create list of features for annotation
        //this list will contain a single element
        listOfFeatures = new ArrayList<>();
        listOfFeatures.add(feature);

        cameraSnapshot();



    }

    public void cameraSnapshot ()
    {
        //create intent to open camera
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //bring up camera and wait for picture to be taken
        startActivityForResult(camera,CAMERA_REQUEST);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //check to see if request camera action return without error(s)
        if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK ){
            //get data returned from camera (intent)
            Bitmap image = (Bitmap) data.getExtras().get("data");

            //assign image from camera to image view
            cameraImage = (ImageView) this.findViewById(R.id.imageScreen);
            cameraImage.setImageBitmap(image);

            //send image to Google Vision Servers for label detection
            visionProcessImage(image,feature);

        }
    }

    public void visionProcessImage(Bitmap image, Feature feature) {
        final List<AnnotateImageRequest> requests = new ArrayList<>();

        AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
        annotateImageReq.setFeatures(listOfFeatures);
        annotateImageReq.setImage(encodeImageForProcessing(image));
        requests.add(annotateImageReq);

        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {

                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer = new VisionRequestInitializer("INSERT_GOOGLE_API_KEY_HERE");

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory,null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(requests);

                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();

                    return response.toString();
                } catch (GoogleJsonResponseException e) {
                    Log.d("Line 135", "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d("Line 137", "failed to make API request because of other IOException " + e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {
                labels = (TextView) findViewById(R.id.labels);
                labels.setText(result);
            }
        }.execute();
    }

    protected Image encodeImageForProcessing(Bitmap image)
    {
        //create new image object
        Image encodedImage = new Image();

        //create bytearrayoutput to be able to write compressed image to
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG,90, byteArrayOutputStream);

        //convert to byte array
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        //encode the bytes into an image
        encodedImage.encodeContent(imageBytes);
        return encodedImage;

    }
}

