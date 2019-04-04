import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.sqs.model.Message;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
package com.liferay;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.*;
import javax.imageio.*;
import java.awt.image.*;
import java.util.stream.Collectors;

public class main {
    public static void main(String[] args) {
        QueueManager myQueue = new QueueManager();
        String myurl = myQueue.getOrCreate("michaelDrorQ1", null);
        List<Message> commends = new ArrayList<>();
        int i, j;
        j=1;
        String msgBody, msg, temp  ;
        msg = "message number: " + j;
        myQueue.sendMessage(msg, myurl);
        while (true) {
            commends=(myQueue.getMessage(null, myurl, false));

            if(commends.size()>0) {
                for( i=0; i<commends.size();i++) {
                    if (commends.get(i).getBody() != null) {
                        msgBody = commends.get(i).getBody();
                        if(msgBody.startsWith("ToImage")==true){
                            try (BufferedInputStream inputStream = new BufferedInputStream(new URL(msgBody.substring(8)).openStream());
                                 FileOutputStream fileOS = new FileOutputStream("/Users/username/Documents/file_name.txt")) {
                                byte data[] = new byte[1024];
                                int byteContent;
                                while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                                    fileOS.write(data, 0, byteContent);
                                }
                            } catch (IOException e) {
                                // handles IO exceptions
                            }


                        }
                        else if(msgBody.startsWith("ToHTML")==true){
                            pdfUrl=msgBody.substring(8);

                        }
                        else if(msgBody.startsWith("ToText")==true){
                            pdfUrl=msgBody.substring(8);

                        }

                        System.out.println(msgBody);
                        myQueue.removeMessage(myurl, commends.get(i));
                    }

                }
                msg = "message number: " + j;
                myQueue.sendMessage(msg, myurl);
                j++;



                }
            }
        }

    }


      //  queueM.sendMessage("hi my man" , myurl);

        //queueM.deleteQueue(queueM.getQueues().get(0), "queue removed");

