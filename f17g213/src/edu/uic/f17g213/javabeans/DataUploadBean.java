package edu.uic.f17g213.javabeans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.fileupload.UploadedFile;

@ManagedBean(name = "dataUpload")
@SessionScoped
public class DataUploadBean {
	private UploadedFile uploadedFile;
	private String fileLabel;
	private int numberRows;
	private String uploadedFileContents;
	private boolean renderUploadResults;
	private String filePath;
	private String tempFileName;
	private FacesContext context;
	private String fileName;
	
	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}
	
	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}
	
	public String getFileLabel() {
		return fileLabel;
	}
	
	public void setFileLabel(String fileLabel) {
		this.fileLabel = fileLabel;
	}
	
	public int getNumberRows() {
		return numberRows;
	}
	public void setNumberRows(int numberRows) {
		this.numberRows = numberRows;
	}
	
	public String getUploadedFileContents() {
		return uploadedFileContents;
	}
	public void setUploadedFileContents(String uploadedFileContents) {
		this.uploadedFileContents = uploadedFileContents;
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public String getTempFileName() {
		return tempFileName;
	}
	
	public void setTempFileName(String tempFileName) {
		this.tempFileName = tempFileName;
	}

	public boolean isRenderUploadResults() {
		return renderUploadResults;
	}

	public void setRenderUploadResults(boolean renderUploadResults) {
		this.renderUploadResults = renderUploadResults;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public FacesContext getContext() {
		return context;
	}
	public void setContext(FacesContext context) {
		this.context = context;
	}
}