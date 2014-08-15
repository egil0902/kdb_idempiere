package org.kanbanboard.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MColumn;
import org.compiere.model.MRefList;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.ValueNamePair;

public class MKanbanBoard extends X_KDB_KanbanBoard{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8437575641565423324L;
	private List<MKanbanStatus> statuses = new ArrayList<MKanbanStatus>();
	private List<MKanbanPriority> priorityRules = new ArrayList<MKanbanPriority>();
	private int numberOfCards =0;
	private boolean isRefList = true;


	public MKanbanBoard(Properties ctx, int KDB_KanbanBoard_ID, String trxName) {
		super(ctx, KDB_KanbanBoard_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public int getNumberOfCards() {
		if(numberOfCards<=0)
			getKanbanCards();
		return numberOfCards;
	}

	public boolean isRefList(){
		if (getKDB_ColumnList_ID()!=0){
			isRefList=true;
		}else if (getKDB_ColumnTable_ID()!=0){
			isRefList=false;
		}
		return isRefList;
	}

	public MColumn getStatusColumn(){
		int columnId = 0;
		if (isRefList())
			columnId = getKDB_ColumnList_ID();
		else
			columnId = getKDB_ColumnTable_ID();

		MColumn column = new MColumn(Env.getCtx(), columnId, get_TrxName());

		return column;

	}

	public void setPrintableNames(){

		if (statuses.size()==0){
			statuses = getStatuses();
		}

		MColumn column = getStatusColumn();
		ValueNamePair list[] = MRefList.getList(getCtx(), column.getAD_Reference_Value_ID(), false);
		if(column.getAD_Reference_Value_ID()!=0&&list.length>0){
			//ValueNamePair list[] = MRefList.getList(getCtx(), column.getAD_Reference_Value_ID(), false);

			//Order the  names by seqNo
			int posStatus;
			for(posStatus=0;posStatus<statuses.size();posStatus++){
				int posList=0;
				boolean match = false;
				while(posList<list.length&&!match){
					if(statuses.get(posStatus).getKDB_StatusListValue().equals(list[posList].getValue())){
						statuses.get(posStatus).setPrintableName(list[posList].toString());
						match=true;
					}
					posList++;
				}
			}
		}else
		{
			MTable table =  MTable.get(getCtx(),column.getReferenceTableName());
			if (table!=null){
				for(MKanbanStatus status: statuses){
					String name = table.get_Translation(status.getName());
					if(name==null)
						name=status.getName();

					status.setPrintableName(name);
				}
			}
		}
	}//setPrintableNames

	public MKanbanStatus getStatus(String statusName){
		for(MKanbanStatus status: statuses){
			String statusN;
			statusN = status.getStatusValue();
			if(statusN.equals(statusName)){
				return status;
			}
		}
		return null;
	}

	public List<MKanbanStatus> getStatuses(){

		if(statuses.size()==0&&getNumberOfStatuses()!=0){
			String sqlSelect = "SELECT kdb_kanbanStatus_id FROM KDB_kanbanStatus WHERE KDB_KanbanBoard_id = ? " +
					"AND IsActive='Y' Order by SeqNo";
			PreparedStatement pstmt = null;
			ResultSet rs = null;

			try{
				pstmt = DB.prepareStatement(sqlSelect, get_TrxName());
				pstmt.setInt(1, getKDB_KanbanBoard_ID());
				rs = pstmt.executeQuery();
				int kanbanStatusesId = 0;
				while(rs.next()){
					kanbanStatusesId = rs.getInt(1);
					MKanbanStatus kanbanStatus = new MKanbanStatus(getCtx(), kanbanStatusesId, get_TrxName());
					statuses.add(kanbanStatus);
				}

			}catch (SQLException e) {
				log.log(Level.SEVERE, sqlSelect , e);
				//throw e;
			} finally {
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}
		}
		return statuses;
	}//getStatuses

	public List<MKanbanPriority> getPriorityRules(){

		if(priorityRules.size()==0){
			String sqlSelect = "SELECT kdb_kanbanpriority_id FROM KDB_kanbanpriority WHERE KDB_KanbanBoard_id = ? " +
					"AND IsActive='Y'";
			PreparedStatement pstmt = null;
			ResultSet rs = null;

			try{
				pstmt = DB.prepareStatement(sqlSelect, get_TrxName());
				pstmt.setInt(1, getKDB_KanbanBoard_ID());
				rs = pstmt.executeQuery();
				int kanbanPriorityId = 0;
				while(rs.next()){
					kanbanPriorityId = rs.getInt(1);
					MKanbanPriority kanbanPriority = new MKanbanPriority(getCtx(), kanbanPriorityId, get_TrxName());
					priorityRules.add(kanbanPriority);
				}

			}catch (SQLException e) {
				log.log(Level.SEVERE, sqlSelect , e);
				//throw e;
			} finally {
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}
		}
		return priorityRules;
	}//getPriorityRules

	public int getNumberOfStatuses(){

		int numberOfStatuses = 0;
		if (statuses.size()==0){
			String sqlSelect = "SELECT COUNT(*) FROM KDB_kanbanStatus WHERE KDB_KanbanBoard_id = ? " +
					"AND IsActive='Y'";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			numberOfStatuses=-1;

			try{
				pstmt = DB.prepareStatement(sqlSelect, get_TrxName());
				pstmt.setInt(1, getKDB_KanbanBoard_ID());
				rs = pstmt.executeQuery();
				if(rs.next())
					numberOfStatuses=rs.getInt(1);

			}catch (SQLException e) {
				log.log(Level.SEVERE, sqlSelect , e);
				//throw e;
			} finally {
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}
		}
		else 		
			numberOfStatuses=statuses.size();

		return numberOfStatuses;
	}//getNumberOfStatuses

	public boolean saveStatuses(){
		for (MKanbanStatus status : statuses) {
			if (status.isActive())
				status.saveEx();
		}
		return true;
	}

	/**
	 *Get every card from the board
	 *and assign them to its respective status
	 */
	public void getKanbanCards(){
		getKanbanCardsInfo();

		if(numberOfCards<=0){
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ");


			MTable table = (MTable) getAD_Table();
			MColumn column = getStatusColumn();
			String llaves[] = table.getKeyColumns();

			sql.append(llaves[0]); 
			sql.append(","+column.getColumnName());

			if(hasPriorityOrder())
				sql.append(", "+getKDB_PrioritySQL());

			sql.append(" FROM "+table.getTableName());

			StringBuilder whereClause = new StringBuilder();
			whereClause.append(" WHERE ");

			if(getWhereClause()!=null)
				whereClause.append(getWhereClause()+" AND ");

			whereClause.append(column.getColumnName()+ " IN ");

			whereClause.append(getInValues());

			whereClause.append(" AND IsActive='Y' ");

			sql.append(whereClause.toString());

			if(hasPriorityOrder())
				sql.append(" ORDER BY "+getKDB_PrioritySQL()+" DESC");

			System.out.println(sql.toString());

			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql.toString(), null);
				rs = pstmt.executeQuery();
				int id = -1;
				String correspondingColumn= null;
				while (rs.next())
				{
					id = rs.getInt(1);
					correspondingColumn = rs.getString(2);
					MKanbanStatus status = getStatus(correspondingColumn);
					MKanbanCard card = new MKanbanCard(id);
					card.setBelongingStatus(status);


					if(hasPriorityOrder()){
						BigDecimal priorityValue = rs.getBigDecimal(3);
						card.setPriorityValue(priorityValue);
					}

					status.addRecord(card);
					numberOfCards++;
				}
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, sql.toString(), e);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}

			//	MTable table = MTable.get(ctx, tableName);

			//	MMailText mailText ;
			//	mailText.setPO(object);
			/*String curr= null;
			for(MKanbanStatus status:getStatuses()){
				if(isRefList)
					curr = status.getKDB_StatusListValue();
				else
					curr= status.getKDB_StatusTableID();	
				getCards(curr, kanbanCardsContent, table);
			}*/

		}
	}//getKanbanCards

	private String getInValues(){

		StringBuilder values = new StringBuilder();
		values.append("(");
		for(MKanbanStatus status:statuses){
			if(isRefList)
				values.append("'"+status.getStatusValue()+"'");
			else
				values.append(status.getStatusValue());

			if(status.equals(statuses.get(statuses.size()-1)))
				values.append(")");
			else 
				values.append(",");
		}

		return values.toString();
	}//getInValues

	boolean hasPriorityOrder(){
		//Check if there's a  valid priority rule 
		if(getKDB_PrioritySQL()!=null) //validar que retorne entero
			return true;
		else
			return false;
	}

	void getKanbanCardsInfo(){
		getKDB_KanbanCard();
		//parsear Texto HTML para obtener los campos que se desean mostrar en las tarjetas
	}

	public void resetCounter() {
		// TODO Auto-generated method stub
		for(MKanbanStatus status:statuses)
			status.setCardNumber(0);

	}
}