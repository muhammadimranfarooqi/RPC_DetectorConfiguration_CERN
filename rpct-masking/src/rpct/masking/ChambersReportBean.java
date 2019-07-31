package rpct.masking;

import java.util.List;

import rpct.db.DataAccessException;



public class ChambersReportBean {
	private final DBDriver dbd;
	private List<LinkBoardWithConfiguration> lboards;
	private List<ChamberWithMasks> chambers;
	
	private final String startDate;
	
	public ChambersReportBean() throws DataAccessException {
		startDate = getDate();
		
		dbd = new DBDriver();
		
		try {
			dbd.configure();
			fetchData();
			dbd.free();
			// TODO: Add report initialization here and uncomment the finally block
		} catch (DataAccessException e) {
			e.printStackTrace();
			dbd.getContext().rollback();
		}
//		finally {
//			dbd.getContext().closeSession();
//		}
	}


	private void fetchData() throws DataAccessException {
		MaskingUtils.log("ChambersReportBean.fetchData() starts");

		lboards = dbd.getAllLinkBoards();

		chambers = dbd.getAllChambers();

		MaskingUtils.log("ChambersReportBean.fetchData() ends");
	}


	public List<LinkBoardWithConfiguration> getAllLBoards() {
		MaskingUtils.log("ChambersReportBean.getAllLBoards() called");

		return lboards; 
	}

	public List<ChamberWithMasks> getAllChambers() {
		MaskingUtils.log("ChambersReportBean.getAllChambers() called");

		return chambers; 
	}
	
	public String getDate() {
		return MaskingUtils.dateNow() + " " + MaskingUtils.timeNow();
	}

	public String getStartDate() {
		return startDate;
	}
}
