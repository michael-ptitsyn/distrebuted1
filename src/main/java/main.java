import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.model.Message;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;



import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.fit.pdfdom.PDFDomTree;

import java.io.*;
import javax.imageio.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLWriter;
import java.awt.image.*;

public class main {
    public static void main(String[] args) {
        QueueManager myQueue = new QueueManager();
        String myurl = myQueue.getOrCreate("michaelDrorQ1", null);
        List<Message> commends = new ArrayList<>();
        int i, j;
        j=1;
        String msgBody, msg, temp  ;
        msg = "ToImage\thttp://www.africau.edu/images/default/sample.pdf" ;
        myQueue.sendMessage(msg, myurl);
        while (true) {
            commends=(myQueue.getMessage(null, myurl, false));

            if(commends.size()>0) {
                for( i=0; i<commends.size();i++) {
                    if (commends.get(i).getBody() != null) {
                        msgBody = commends.get(i).getBody();
                        if(msgBody.startsWith("ToImage")==true){
                            try {
                                        InputStream input = new URL(msgBody.substring(8)).openStream();

                                PDDocument pdf = PDDocument.load(input);
                                PDFRenderer pdfRenderer = new PDFRenderer (pdf);
                                BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 30, ImageType.RGB);
                                String name=msgBody.substring(8, msgBody.length()-4);
                                ImageIOUtil.writeImage(bim, name + ".png", 30);

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write( bim, "png", baos );
                                baos.flush();
                                byte[] imageInByte = baos.toByteArray();
                                baos.close();

                                FileUtils.writeByteArrayToFile(new File(name), imageInByte);
                                S3Client man =new S3Client();
                                man.uploadFile(Constants.BUCKET_NAME, 1, )


                            } catch (InvalidPasswordException e) {
                                // TODO CHANGE THE NAME OF THE FILE SO WE DONT RUN OVER THE SAME FILE EVERY TIME
                                e.printStackTrace();
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }


                        }
                        else if(msgBody.startsWith("ToHTML")==true){
                        try {
                            String name = msgBody.substring(8, msgBody.length() - 4);
                            InputStream input = new URL(msgBody.substring(8)).openStream();
                            PDDocument pdf = PDDocument.load(input);
                            PDDocument fpdf = new PDDocument();
                            fpdf.addPage( pdf.getPage(0));

                            PDFTextStripper striper= new PDFTextStripper();
                            name=name+"html";
                            File FFile = new File();
                            FileWriter writer= new FileWriter(FFile);

                            striper.writeText(fpdf, writer );
                            writer.
                            S3Client man =new S3Client();
                            man.uploadFile(Constants.BUCKET_NAME, name , FFile );


                        } catch (InvalidPasswordException e) {
                            e.printStackTrace();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                       else if(msgBody.startsWith("ToText")==true){
                        try {
                            String name = msgBody.substring(8, msgBody.length() - 4);
                            InputStream input = new URL(msgBody.substring(8)).openStream();
                            PDDocument pdf = PDDocument.load(input);
                            PDDocument fpdf = new PDDocument();
                            fpdf.addPage( pdf.getPage(0));

                            PDFTextStripper striper= new PDFTextStripper();
                            FileWriter writer= new FileWriter(new File(name+".txt"));
                            striper.writeText(fpdf, writer );

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                        System.out.println(msgBody);
                        myQueue.removeMessage(myurl, commends.get(i));
                    }

                }

                myQueue.sendMessage(msg, myurl);
                j++;



                }
            }
        }

    }


      //  queueM.sendMessage("hi my man" , myurl);

        //queueM.deleteQueue(queueM.getQueues().get(0), "queue removed");

