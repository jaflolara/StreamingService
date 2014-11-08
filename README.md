StreamingService
================

A DataHandler for SOAP web services using MTOM to stream large amounts of records encoded as protocol buffers. The client processes records while the server is still sending.

The tests contain an example web service that transfers a million small protocol buffers in a few seconds with the client and server running on the same machine.

In the web service interface you must define the result type of the web method to be a StreamingResponse of the desired record type:

    @WebMethod(operationName = "snafucate")
    public StreamingResponse<Record> snafucate(int i) throws IOException;

In the web service implementation you must enable MTOM and streaming attachments:

    @WebService(endpointInterface = "org.avidj.snafu.sss.SnafucationWS")
    @MTOM // enable the MTOM feature which allows to parse rows at the client while server is sending
    @StreamingAttachment(parseEagerly = false, dir = "/tmp", memoryThreshold = 4000000L)
    @Service
    public class SnafucationWSImpl implements SnafucationWS {

On the client you must also enable MTOM and streaming attachments:

    private SnafucationWS connect() {
        Service result = null;
        try {
            result = Service.create(
                    new URL(serviceUrl + "?wsdl"), 
                    new QName("http://sss.snafu.avidj.org/", "SnafucationWSImplService"));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not create web service endpoint.", e);
        }
        SnafucationWS port = result.getPort(SnafucationWS.class,
                // Enable MTOM at the client for transmission of binary data
                new MTOMFeature(),
                // Load off attachments to the file system when exceeding 4MB in size.
                new StreamingAttachmentFeature("/tmp", false, 4000000L));
        return port;
    }
