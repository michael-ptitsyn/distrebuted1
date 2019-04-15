//package aws;
//
//import java.util.List;
//
//import com.amazonaws.SdkBaseException;
//import com.amazonaws.services.simpledb.AmazonSimpleDB;
//import com.amazonaws.services.simpledb.AmazonSimpleDBClientBuilder;
//import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
//import com.amazonaws.services.simpledb.model.CreateDomainRequest;
//import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
//import com.amazonaws.services.simpledb.model.Item;
//import com.amazonaws.services.simpledb.model.ReplaceableItem;
//import com.amazonaws.services.simpledb.model.SelectRequest;
//
//public class S3manager extends AwsManager{
//    private AmazonSimpleDB sdb;
//    private List<String> domainNames;
//    public S3manager() {
//        sdb = AmazonSimpleDBClientBuilder.standard()
//                .withCredentials(credentialsProvider)
//                .withRegion("us-west-2")
//                .build();
//        domainNames = sdb.listDomains().getDomainNames();
//
//    }
//
//    public void creatDomain(String domainName) throws SdkBaseException {
//        sdb.createDomain(new CreateDomainRequest(domainName));
//        domainNames.add(domainName);
//    }
//
//    public List<Item> select(String expression){
//        SelectRequest selectRequest = new SelectRequest(expression);
//        return sdb.select(selectRequest).getItems();
//
//    }
//
//    public void put(String domainName, List<ReplaceableItem> data) throws SdkBaseException {
//        sdb.batchPutAttributes(new BatchPutAttributesRequest(domainName, data));
//
//    }
//
//    public void deleteItem(String domain,String itemName) throws SdkBaseException{
//        sdb.deleteAttributes(new DeleteAttributesRequest(domain, itemName));
//    }
//
//    public List<String> getDomainNames() throws SdkBaseException{
//        if(domainNames!= null)
//            return domainNames;
//        domainNames = sdb.listDomains().getDomainNames();
//        return domainNames;
//    }
//
//    public AmazonSimpleDB getSdb() {
//        return sdb;
//    }
////    public static void main(String[] args) throws Exception {
////            try {
////                // Create a domain
////                String myDomain = "MyStore";
////                System.out.println("Creating domain called " + myDomain + ".\n");
////
////
////                // List domains
////                System.out.println("Listing all domains in your account:\n");
////                for (String domainName : ) {
////                    System.out.println("  " + domainName);
////                }
////                System.out.println();
//
//                // Put data into a domain
////                System.out.println("Putting data into " + myDomain + " domain.\n");
////                sdb.batchPutAttributes(new BatchPutAttributesRequest(myDomain, createSampleData()));
//
//                // Select data from a domain
//                // Notice the use of backticks around the domain name in our select expression.
////                String selectExpression = "select * from `" + myDomain + "` where Category = 'Clothes'";
////                System.out.println("Selecting: " + selectExpression + "\n");
////
////                System.out.println();
//
//                // Delete values from an attribute
////                System.out.println("Deleting Blue attributes in Item_O3.\n");
////                Attribute deleteValueAttribute = new Attribute("Color", "Blue");
////                sdb.deleteAttributes(new DeleteAttributesRequest(myDomain, "Item_03")
////                        .withAttributes(deleteValueAttribute));
//
//                // Delete an attribute and all of its values
////                System.out.println("Deleting attribute Year in Item_O3.\n");
////                sdb.deleteAttributes(new DeleteAttributesRequest(myDomain, "Item_03")
////                        .withAttributes(new Attribute().withName("Year")));
//
//                // Replace an attribute
////                System.out.println("Replacing Size of Item_03 with Medium.\n");
////                List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>();
////                replaceableAttributes.add(new ReplaceableAttribute("Size", "Medium", true));
////                sdb.putAttributes(new PutAttributesRequest(myDomain, "Item_03", replaceableAttributes));
//
//                // Delete an item and all of its attributes
//                //System.out.println("Deleting Item_03.\n");
////                sdb.deleteAttributes(new DeleteAttributesRequest(myDomain, "Item_03"));
//
//                // Delete a domain
//                //System.out.println("Deleting " + myDomain + " domain.\n");
//                //sdb.deleteDomain(new DeleteDomainRequest(myDomain));
////        }
////
////        /**
////         * Creates an array of SimpleDB ReplaceableItems populated with sample data.
////         *
////         * @return An array of sample item data.
////         */
////        private static List<ReplaceableItem> createSampleData() {
////            List<ReplaceableItem> sampleData = new ArrayList<ReplaceableItem>();
////
////            sampleData.add(new ReplaceableItem("Item_01").withAttributes(
////                    new ReplaceableAttribute("Category", "Clothes", true),
////                    new ReplaceableAttribute("Subcategory", "Sweater", true),
////                    new ReplaceableAttribute("Name", "Cathair Sweater", true),
////                    new ReplaceableAttribute("Color", "Siamese", true),
////                    new ReplaceableAttribute("Size", "Small", true),
////                    new ReplaceableAttribute("Size", "Medium", true),
////                    new ReplaceableAttribute("Size", "Large", true)));
////
////            sampleData.add(new ReplaceableItem("Item_02").withAttributes(
////                    new ReplaceableAttribute("Category", "Clothes", true),
////                    new ReplaceableAttribute("Subcategory","Pants", true),
////                    new ReplaceableAttribute("Name", "Designer Jeans", true),
////                    new ReplaceableAttribute("Color", "Paisley Acid Wash", true),
////                    new ReplaceableAttribute("Size", "30x32", true),
////                    new ReplaceableAttribute("Size", "32x32", true),
////                    new ReplaceableAttribute("Size", "32x34", true)));
////
////            sampleData.add(new ReplaceableItem("Item_03").withAttributes(
////                    new ReplaceableAttribute("Category", "Clothes", true),
////                    new ReplaceableAttribute("Subcategory", "Pants", true),
////                    new ReplaceableAttribute("Name", "Sweatpants", true),
////                    new ReplaceableAttribute("Color", "Blue", true),
////                    new ReplaceableAttribute("Color", "Yellow", true),
////                    new ReplaceableAttribute("Color", "Pink", true),
////                    new ReplaceableAttribute("Size", "Large", true),
////                    new ReplaceableAttribute("Year", "2006", true),
////                    new ReplaceableAttribute("Year", "2007", true)));
////
////            sampleData.add(new ReplaceableItem("Item_04").withAttributes(
////                    new ReplaceableAttribute("Category", "Car Parts", true),
////                    new ReplaceableAttribute("Subcategory", "Engine", true),
////                    new ReplaceableAttribute("Name", "Turbos", true),
////                    new ReplaceableAttribute("Make", "Audi", true),
////                    new ReplaceableAttribute("Model", "S4", true),
////                    new ReplaceableAttribute("Year", "2000", true),
////                    new ReplaceableAttribute("Year", "2001", true),
////                    new ReplaceableAttribute("Year", "2002", true)));
////
////            sampleData.add(new ReplaceableItem("Item_05").withAttributes(
////                    new ReplaceableAttribute("Category", "Car Parts", true),
////                    new ReplaceableAttribute("Subcategory", "Emissions", true),
////                    new ReplaceableAttribute("Name", "O2 Sensor", true),
////                    new ReplaceableAttribute("Make", "Audi", true),
////                    new ReplaceableAttribute("Model", "S4", true),
////                    new ReplaceableAttribute("Year", "2000", true),
////                    new ReplaceableAttribute("Year", "2001", true),
////                    new ReplaceableAttribute("Year", "2002", true)));
////
////            return sampleData;
////        }
////    }
//}
