

package org.opendaylight.controller.flowstoragemanager.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
//import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.flowstoragemanager.IFlowStorage;
import org.opendaylight.controller.flowstoragemanager.IFlowUpdateListener;
//import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Class that manages forwarding rule installation and removal in the flow storage database
 */
public class FlowStorageManager implements
        IFlowUpdateListener{

    private static final Logger log = LoggerFactory.getLogger(FlowStorageManager.class);
    private IFlowStorage dbstorage = null;
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
    private static final String flowTblName = "flow";//flow table name
    private static final String matchTblName = "match";//match table name
    private static String actiontblname = "action";
    private static String protocoltblname = "Protocol";
    private static String notetypetblname = "nodetype";
    private static String statistictblname = "statistic";
    private Node node = null;
    private Flow flow;
    //private List<Match> matchList;
    private List<Action> actions;
    private String nodeType;
    private String nodeId;
    private long flowId;
    private String setupTime;
    private long reflowid;
    private short entryPriority;
    private Match match;
    private List<MatchType> matchTypes;
    private List<MatchField> matchFields;
    private String actionsStr = null;
    private String matchTypesStr = null;
    private String matchValue =null;
    private String matchValueStr=null;
    private String protocolValueStr = "";
    private long reMatchId; //insert match table return this value
    private long reprotocolId;
    private long reactionId;
    private long renodetypeId;
    private long restatisticId;
    private long bytecount;
    private int durationSeconds;
    private int durationNanoseconds;
    private String reason;
    private long packet;
    private int restatisticid;
    private long reid;
    ArrayList<FlowEntry> flowDB1 = new ArrayList<FlowEntry>();
    ArrayList<FlowEntry> flowDB2 = new ArrayList<FlowEntry>();
    private int Switch =0;
    //private long recvBytes;
    //private long recvPackets;

    private Connection conn = null;
    //private PreparedStatement pstmt = null;
    private ResultSet rs = null;
    //get db storage instance
    void start() {
        conn = null;
        rs = null;
        dbstorage = (IFlowStorage) ServiceHelper.getGlobalInstance(
                IFlowStorage.class, this);
        if (dbstorage == null) {
            throw new ServiceUnavailableException(RestMessages.SERVICEUNAVAILABLE.toString());
        }else{
            conn = dbstorage.connectDb();
        }
        timer();
    }

   // PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     */
      void stop(){
          if (dbstorage == null) {
              throw new ServiceUnavailableException(RestMessages.SERVICEUNAVAILABLE.toString());
          }else{
              dbstorage.disConnectDb();
              conn = null;
          }
      }

    private String getMatchValue(String str){
        String matchValue;
        int beginIndex = str.indexOf('(') +1;
        int endIndex = str.lastIndexOf(')');
        matchValue = str.substring(beginIndex,endIndex);
        return matchValue;
    }

    private String getSysTime(){
        Date date=new Date();
        String time=null;
        time= formatter.format(date);
        return time;
    }

    private String generateAddMatchSql(){
        String matchSql;
        //insert into matches table a new match
        if(reprotocolId != 0){
            matchSql =
                    "INSERT INTO "+ "`" + matchTblName+ "`" + "("+matchTypesStr+ ")"
                    +"VALUES" + "(" + matchValueStr + ")";
        }else{
            matchSql =
                    "INSERT INTO "+ "`" + matchTblName + "`" + "(" + matchTypesStr + "," + "NW_PROTO" + ")"
                    +"VALUES" + "(" + matchValueStr +"," + reprotocolId + ")";
        }
        return matchSql;
    }

    private String generateAddFlowSql(){
        String flowSql;
        //insert into flow table a new flow Entry
        flowSql =
                 "INSERT INTO "+ "`" + flowTblName +"`"
                   + "("
                  + "node_id,"
                  + "node_type,"
                  + "flow_id,"
                  //+ "stop_time,"
                  + "entry_priority,"
                  //+ "recv_pkts,"
                  //+ "recv_bytes,"
                  + "actions,"
                  + "match_id,"
                  + "statistic_id"
                  + ")"
                 +"VALUES"
                  + "("
                  + "\'"+nodeId+"\'"+","
                  + renodetypeId+","
                  + flowId+","
                  + entryPriority+","
                  //+ null+","
                  //+ null+","
                  + reactionId+","
                  + reMatchId+","
                  + restatisticId
                  + ")";
        return flowSql;
    }

    private String generateAddActionSql(){
        String actionSql;
        actionSql =
                "INSERT INTO "+ "`" + actiontblname +"`"
                + "("
                + "actions"
                +")"
                +"VALUES"
                + "("
                + "\'"+actionsStr+"\'"
                + ")";
        return actionSql;
    }

    private String generateAddprotocolSql(){
        String protocolSql;
        protocolSql =
               "INSERT INTO "+ "`" + protocoltblname +"`"
               + "("
               + "NW_PROTO"
               + ")"
               +"VALUES"
               + "("
               + "\'"+protocolValueStr+"\'"
               + ")";
        return protocolSql;
    }

    private String generateAddnodetypeSql(){
        String nodetypeSql;
        nodetypeSql =
               "INSERT INTO "+ "`" + notetypetblname +"`"
               + "("
               + "node_type"
               + ")"
               +"VALUES"
               + "("
               + "\'"+nodeType+"\'"
               + ")";
        return nodetypeSql;
    }

    private String generateAddstatisticSql(){
        String statisticSql;
        statisticSql =
                "INSERT INTO "+ "`" + statistictblname +"`"
                + "("
                + "setup_time"
                +")"
                +"VALUES"
                + "("
                + "\'"+setupTime+"\'"
                + ")";
        return statisticSql;
    }

    private String generateUpdatestatisticSql(){
        String updateSql;
        //insert into flow table a new flow Entry
        updateSql =
            "UPDATE "+ statistictblname+" "
            +"SET "
            + "durationSeconds="
            + "\'"+durationSeconds+"\'"+","
            + "durationNanoseconds="
            + "\'"+durationNanoseconds+"\'"+","
            + "recv_pkts="
            + "\'"+packet+"\'"+","
            + "recv_bytes="
            + "\'"+bytecount+"\'"+","
            + "reason="
            + "\'"+reason+"\'"
            + "WHERE statistic_id="
            + restatisticid ;
        return updateSql;
    }
    private String generategetstatisticidSql(){
        String getidSql;
        getidSql = "SELECT "+ "statistic_id"+" "+ "FROM "+"`"+"flow"+"`"+" "+"WHERE "+ "id="+reid ;
        return getidSql;
    }
    private String generategetflowidSql(){
        String getidSql;
        getidSql = "SELECT "+ "id "+" "+ "FROM "+"`"+"flow"+"`"+" "+"WHERE "+ "flow_id="+reflowid+" "+"order by id desc limit 1" ;
        return getidSql;
    }
    private void parseFlowEntry(FlowEntry flowEntry){
        //get node
        node = flowEntry.getNode();
        nodeType = node.getType();
        nodeId = node.getNodeIDString();
        //get hash
        flowId = flowEntry.hashCode();
        //get flow
        flow = flowEntry.getFlow();
        //get flow entry priority
        entryPriority = flow.getPriority();
        //get flow match
        match = flow.getMatch();
        //get match type list
        matchTypes = match.getMatchesList();
        //get mach value list
        matchFields = match.getMatchFields();
        //get actions
        actions = flow.getActions();
        //get match type string
        matchValue = "";
        matchValueStr= "";
        matchTypesStr = ""; //null ---> ""
        //protocolTypeStr="";
        protocolValueStr="";
        for(MatchType name : matchTypes){
            if(name.toString().equals("NW_PROTO")){
                name.toString();
            }
            else{
                matchTypesStr += "," + name ;
            //matchType.toString();
            }
        }
        int beginIndex, endIndex;
        beginIndex = 1;
        endIndex = matchTypesStr.length();
        matchTypesStr = matchTypesStr.substring(beginIndex,endIndex);
        //get match value string
        for(MatchField name : matchFields){
            if(name.toString().equals("NW_PROTO(4)")){
                protocolValueStr=getMatchValue(name.toString());
            }
            else{
                matchValue = getMatchValue(name.toString());
                matchValueStr += "," + "'" + matchValue + "'";
            }
        }
        beginIndex = 1;
        endIndex = matchValueStr.length();
        matchValueStr = matchValueStr.substring(beginIndex,endIndex);

        actionsStr = "";
        for(Action name :actions){
            String strname = name.toString();
            actionsStr += "," + strname;
        }
        beginIndex = 1;
        endIndex = actionsStr.length();
        actionsStr = actionsStr.substring(beginIndex,endIndex);
    }
    /**
     * monitor the addEntry operation in FRM and will update flow database accordingly.
     *
     */
    public void writeflowEntry(FlowEntry flowEntry){
        if(Switch ==0){
            flowDB1.add(flowEntry);
            if(flowDB1.size()==4){
                Switch = 1;
                this.addFlowList(flowDB1);
                flowDB1.clear();
            }
        }else if(Switch ==1){
            flowDB2.add(flowEntry);
            if(flowDB2.size()==4){
                Switch = 0;
                this.addFlowList(flowDB2);
                flowDB2.clear();
            }
        }
    }
    public void addFlowList(ArrayList<FlowEntry> flowDB){
        if (dbstorage == null) {
            dbstorage = (IFlowStorage) ServiceHelper.getGlobalInstance(
                    IFlowStorage.class, this);
        }
            if(conn == null){
                conn = dbstorage.connectDb();
            }
            for(int i=0;i<flowDB.size();i++){
                FlowEntry a = flowDB.get(i);
                this.addFlowEntry(a);
            }
            if (conn != null){
                dbstorage.disConnectDb();
                conn=null;
            }
        }
    public void timer(){
         Calendar calendar = Calendar.getInstance();
         calendar.set(Calendar.HOUR_OF_DAY, 12);
         calendar.set(Calendar.MINUTE, 0);
         calendar.set(Calendar.SECOND, 0);

         Date time = calendar.getTime();

         Timer timer = new Timer();
         timer.scheduleAtFixedRate(new TimerTask() {
             public void run() {
                 if(Switch == 0){
                     addFlowList(flowDB1);
                     flowDB1.clear();
                 }else if(Switch == 1){
                     addFlowList(flowDB2);
                     flowDB2.clear();
                 }
             }
         },time, 1000 * 60 * 60 * 24);
    }
    public void addFlowEntry(FlowEntry flowEntry){
         String matchSql, flowSql ,actionSql ,protocolSql ,nodetypeSql, statisticSql;
         PreparedStatement pstmtMatch=null;
         PreparedStatement pstmtFlow=null;
         PreparedStatement pstmtaction=null;
         PreparedStatement pstmtprotocol = null;
         PreparedStatement pstmtnodetypeSql = null;
         PreparedStatement pstmtstatisticSql = null;
             //if not connect with database then try connect
             //get flowEntry setup time
             setupTime = getSysTime();
             //parse flow entry
             parseFlowEntry(flowEntry);
             //generate insert match sql
             protocolSql = generateAddprotocolSql();
             try{
                 pstmtprotocol = conn.prepareStatement(protocolSql, Statement.RETURN_GENERATED_KEYS);
                 conn.setAutoCommit(false);
                 pstmtprotocol.executeUpdate();
                 rs = pstmtprotocol.getGeneratedKeys();
                 //System.out.println("The value is ");
                 if(rs.next()) {
                     //System.out.println(rs.getInt(1));
                     //get returned max match_id
                     reprotocolId =  rs.getInt(1);
                 }
                 matchSql = generateAddMatchSql();
                 pstmtMatch = conn.prepareStatement(matchSql, Statement.RETURN_GENERATED_KEYS);
                 conn.setAutoCommit(false);
                 pstmtMatch.executeUpdate();
                 rs = pstmtMatch.getGeneratedKeys();
                 //System.out.println("The value is ");
                 if(rs.next()) {
                     //System.out.println(rs.getInt(1));
                     //get returned max match_id
                     reMatchId =  rs.getInt(1);
                 }
                 actionSql = generateAddActionSql();
                 pstmtaction = conn.prepareStatement(actionSql,Statement.RETURN_GENERATED_KEYS);
                 conn.setAutoCommit(false);
                 pstmtaction.executeUpdate();
                 rs = pstmtaction.getGeneratedKeys();
                 //System.out.println("The value is ");
                 if(rs.next()) {
                     //System.out.println(rs.getInt(1));
                     //get returned max match_id
                     reactionId =  rs.getInt(1);
                 }
                 nodetypeSql = generateAddnodetypeSql();
                 pstmtnodetypeSql = conn.prepareStatement(nodetypeSql,Statement.RETURN_GENERATED_KEYS);
                 conn.setAutoCommit(false);
                 pstmtnodetypeSql.executeUpdate();
                 rs = pstmtnodetypeSql.getGeneratedKeys();
                 //System.out.println("The value is ");
                 if(rs.next()) {
                     //System.out.println(rs.getInt(1));
                     //get returned max match_id
                     renodetypeId =  rs.getInt(1);
                 }
                 statisticSql = generateAddstatisticSql();
                 pstmtstatisticSql = conn.prepareStatement(statisticSql,Statement.RETURN_GENERATED_KEYS);
                 conn.setAutoCommit(false);
                 pstmtstatisticSql.executeUpdate();
                 rs = pstmtstatisticSql.getGeneratedKeys();
                 //System.out.println("The value is ");
                 if(rs.next()) {
                     //System.out.println(rs.getInt(1));
                     //get returned max match_id
                     restatisticId =  rs.getInt(1);
                 }
                 flowSql = generateAddFlowSql();
                 pstmtFlow = conn.prepareStatement(flowSql);
                 pstmtFlow.executeUpdate();
                 conn.commit();
             }catch(SQLException e) {
                 System.out.println("SQLException");
             }
    }

    /**
     * monitor the removeEntry operation in FRM and will update flow database accordingly.
     *
     */
    public int updateFlowEntry(ConcurrentMap<Integer, Object[]> statistic){
         if(Switch == 0){
             addFlowList(flowDB1);
             flowDB1.clear();
         }else if(Switch == 1){
             addFlowList(flowDB2);
             flowDB2.clear();
         }
         Set<Entry<Integer, Object[]>> statistics = statistic.entrySet();
         Iterator<Entry<Integer, Object[]>> iterator = statistics.iterator();
         while(iterator.hasNext()){
             Entry<Integer, Object[]> entry = (Entry<Integer, Object[]>)iterator.next();
             reflowid =(long) entry.getKey();
             Object[] c = entry.getValue();
             bytecount = (long)c[0];
             durationSeconds = (int)c[2];
             durationNanoseconds = (int)c[3];
             reason = "NULL";
             packet = (long)c[1];
             String getidSql;
             String updateSql;
             String getflowidSql;
             PreparedStatement pstmtupdate=null;
             PreparedStatement pstmtgetidSql = null;
             PreparedStatement pstmtgetflowidSql = null;
             if (dbstorage == null) {
                 dbstorage = (IFlowStorage) ServiceHelper.getGlobalInstance(
                         IFlowStorage.class, this);
             }
                 //if not connect with database then try connect
                 if(conn == null){
                     conn = dbstorage.connectDb();
                 }
                 //generate update flow entry stop time sql
                 getflowidSql = generategetflowidSql();
                 try{
                     conn.setAutoCommit(false);
                     conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                     pstmtgetflowidSql = conn.prepareStatement(getflowidSql);
                     rs = pstmtgetflowidSql.executeQuery();
                     //System.out.println("The value is ");
                     if(rs.next()) {
                         //System.out.println(rs.getInt(1));
                         //get returned max match_id
                         reid =  rs.getInt(1);
                     }
                     getidSql = generategetstatisticidSql();
                     conn.setAutoCommit(false);
                     conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                     pstmtgetidSql = conn.prepareStatement(getidSql);
                     rs = pstmtgetidSql.executeQuery();
                     //System.out.println("The value is ");
                     if(rs.next()) {
                         //System.out.println(rs.getInt(1));
                         //get returned max match_id
                         restatisticid =  rs.getInt(1);
                     }
                     updateSql = generateUpdatestatisticSql();
                     pstmtupdate = conn.prepareStatement(updateSql);
                     conn.setAutoCommit(false);
                     pstmtupdate.executeUpdate();
                     conn.commit();
                 }catch(SQLException e) {
                     System.out.println("SQLException");
                 } finally {
                     try {
                         if (conn != null){
                             dbstorage.disConnectDb();
                             conn=null;
                         }
                         if(pstmtupdate != null) {
                             pstmtupdate.close();
                             pstmtupdate = null;
                         }
                     } catch(SQLException e) {
                         System.out.println("Database connection load failure");
                     }
                 }
         }
     return 1;
    }
}
