package managers.mensajes.adjuntos;

public class AdjuntoDTO {

    private String filename;
    private String mimeType;

    private String cipherTextBase64;
    private String encKeyBase64;
    private String ivBase64;

    public AdjuntoDTO() {
    }

    public AdjuntoDTO(String filename, String mimeType, String cipherTextBase64, String encKeyBase64, String ivBase64) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.cipherTextBase64 = cipherTextBase64;
        this.encKeyBase64 = encKeyBase64;
        this.ivBase64 = ivBase64;
    }

    /* getters / setters */

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getCipherTextBase64() {
        return cipherTextBase64;
    }

    public void setCipherTextBase64(String cipherTextBase64) {
        this.cipherTextBase64 = cipherTextBase64;
    }

    public String getEncKeyBase64() {
        return encKeyBase64;
    }

    public void setEncKeyBase64(String encKeyBase64) {
        this.encKeyBase64 = encKeyBase64;
    }

    public String getIvBase64() {
        return ivBase64;
    }

    public void setIvBase64(String ivBase64) {
        this.ivBase64 = ivBase64;
    }
}
