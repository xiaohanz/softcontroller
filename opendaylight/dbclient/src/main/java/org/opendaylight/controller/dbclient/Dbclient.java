package org.opendaylight.controller.dbclient;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.opendaylight.controller.flowstoragemanager.IFlowStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dbclient implements IFlowStorage{
        private Connection conn = null;
        private static String port = System.getProperty("DBport");
        private static String DBname = System.getProperty("DBname");
        private static String username = System.getProperty("username");
        private static String password = System.getProperty("password");
        private static String characterEncoding = System.getProperty("characterEncoding");
        private static String driver = System.getProperty("driver");
        private static final String url = "jdbc:mysql://localhost:"+port+"/"+DBname+"?user="+username+"&password="+password+"&useUnicode=true&&characterEncoding="+characterEncoding+"&autoReconnect = true";
        private String user = null;
        private static final Logger log = LoggerFactory.getLogger(Dbclient.class);
        /**
         * Function called by the dependency manager when all the required
         * dependencies are satisfied
         *
         */
        void init() {
            try {
                //java.sql.DriverManager.registerDriver(new Driver());
                Class.forName(driver).newInstance();
                System.out.println("********************"+'\n'+'\n'+'\n'+'\n');
                try {
                    if(conn == null){
                        this.connectDb();
                    }
                    ResultSet rs = conn.getMetaData().getTables(null, null, "action", null);
                    if (rs.next()){
                        System.out.println("action table has been established");
                    }else{
                        System.out.println("action table is not established");
                        System.out.println("Creating the action table");
                        PreparedStatement pstmtaction=null;
                        String actionSql = createactiontable();
                        pstmtaction = conn.prepareStatement(actionSql,Statement.RETURN_GENERATED_KEYS);
                        conn.setAutoCommit(false);
                        pstmtaction.executeUpdate();
                        conn.commit();
                        System.out.println("action table has been established");
                    }
                } catch (SQLException e) {
                    System.out.println("Database connection load failure");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    if(conn == null){
                        this.connectDb();
                    }
                    ResultSet rs = conn.getMetaData().getTables(null, null, "nodetype", null);
                    if (rs.next()){
                        System.out.println("nodetype table has been established");
                    }else{
                        System.out.println("nodetype table is not established");
                        System.out.println("Creating the nodetype table");
                        PreparedStatement pstmtnodetype=null;
                        String nodetypeSql = this.createnodetypetable();
                        pstmtnodetype = conn.prepareStatement(nodetypeSql,Statement.RETURN_GENERATED_KEYS);
                        conn.setAutoCommit(false);
                        pstmtnodetype.executeUpdate();
                        conn.commit();
                        System.out.println("nodetype table has been established");
                    }
                } catch (SQLException e) {
                    System.out.println("Database connection load failure");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    if(conn == null){
                        this.connectDb();
                    }
                    ResultSet rs = conn.getMetaData().getTables(null, null, "protocol", null);
                    if (rs.next()){
                        System.out.println("protocol table has been established");
                    }else{
                        System.out.println("protocol table is not established");
                        System.out.println("Creating the protocol table");
                        PreparedStatement pstmtprotocol=null;
                        String protocolSql = this.createprotocoltable();
                        pstmtprotocol = conn.prepareStatement(protocolSql,Statement.RETURN_GENERATED_KEYS);
                        conn.setAutoCommit(false);
                        pstmtprotocol.executeUpdate();
                        conn.commit();
                        System.out.println("protocol table has been established");
                    }
                } catch (SQLException e) {
                    System.out.println("Database connection load failure");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    if(conn == null){
                        this.connectDb();
                    }
                    ResultSet rs = conn.getMetaData().getTables(null, null, "statistic", null);
                    if (rs.next()){
                        System.out.println("statistic table has been established");
                    }else{
                        System.out.println("statistic table is not established");
                        System.out.println("Creating the statistic table");
                        PreparedStatement pstmtstatistic=null;
                        String statisticSql = this.createstatistictable();
                        pstmtstatistic = conn.prepareStatement(statisticSql,Statement.RETURN_GENERATED_KEYS);
                        conn.setAutoCommit(false);
                        pstmtstatistic.executeUpdate();
                        conn.commit();
                        System.out.println("statistic table has been established");
                    }
                } catch (SQLException e) {
                    System.out.println("Database connection load failure");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    if(conn == null){
                        this.connectDb();
                    }
                    ResultSet rs = conn.getMetaData().getTables(null, null, "match", null);
                    if (rs.next()){
                        System.out.println("match table has been established");
                    }else{
                        System.out.println("match table is not established");
                        System.out.println("Creating the match table");
                        PreparedStatement pstmtmatch=null;
                        String matchSql = this.creatematchtable();
                        pstmtmatch = conn.prepareStatement(matchSql,Statement.RETURN_GENERATED_KEYS);
                        conn.setAutoCommit(false);
                        pstmtmatch.executeUpdate();
                        conn.commit();
                        System.out.println("match table has been established");
                    }
                } catch (SQLException e) {
                    System.out.println("Database connection load failure");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    if(conn == null){
                        this.connectDb();
                    }
                    ResultSet rs = conn.getMetaData().getTables(null, null, "flow", null);
                    if (rs.next()){
                        System.out.println("flow table has been established");
                    }else{
                        System.out.println("flow is not established");
                        System.out.println("Creating the flow table");
                        PreparedStatement pstmtflow=null;
                        String flowSql = this.createflowtable();
                        pstmtflow = conn.prepareStatement(flowSql,Statement.RETURN_GENERATED_KEYS);
                        conn.setAutoCommit(false);
                        pstmtflow.executeUpdate();
                        PreparedStatement pstmtflowkey=null;
                        String flowkeySql = this.createflowkeytable();
                        pstmtflowkey = conn.prepareStatement(flowkeySql,Statement.RETURN_GENERATED_KEYS);
                        conn.setAutoCommit(false);
                        pstmtflowkey.executeUpdate();
                        conn.commit();
                        System.out.println("flow table has been established");
                    }
                } catch (SQLException e) {
                    System.out.println("Database connection load failure");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                System.out.println("Load the driver failed");
                e.printStackTrace();
            }
        }
        //connect to mySql server
        public Connection connectDb(){
            try {
                 conn = DriverManager.getConnection(url,user,password);
            } catch (SQLException e) {
                System.out.println("Database connection load failure");
                e.printStackTrace();
                return null;
            }
            return conn;
        }
        //disconnect from the mySql server
        public void disConnectDb(){
            try {
              if(conn != null) {
                  conn.close();
                  conn = null;
             }
            } catch(Exception e) {
             System.out.println("Database connection load failure");
             e.printStackTrace();
            }
        }
        //
        /**
         * Function called by dependency manager after "init ()" is called
         * and after the services provided by the class are registered in
         * the service registry
         *
         */
        void start() {
        }

        /**
         * Function called by the dependency manager before the services
         * exported by the component are unregistered, this will be
         * followed by a "destroy ()" calls
         */
        void stop(){
        }
        public String createactiontable(){
            String actionSql;
            actionSql="create table `action`(`action_id` int(11) NOT NULL AUTO_INCREMENT,`actions` text DEFAULT NULL,PRIMARY KEY(`action_id`))DEFAULT CHARSET=utf8";
            return actionSql;
        }
        public String createnodetypetable(){
            String nodetypeSql;
            nodetypeSql="create table `nodetype`(`type_id` int(11) NOT NULL AUTO_INCREMENT,`node_type` varchar(255) DEFAULT NULL,PRIMARY KEY(`type_id`))DEFAULT CHARSET=utf8";
            return nodetypeSql;
        }
        public String createprotocoltable(){
            String protocolSql;
            protocolSql="create table `protocol`(`Protocol_id` int(11) NOT NULL AUTO_INCREMENT,`NW_PROTO` varchar(255) DEFAULT NULL,PRIMARY KEY(`Protocol_id`))DEFAULT CHARSET=utf8";
            return protocolSql;
        }
        public String createstatistictable(){
            String statisticSql;
            statisticSql="create table `statistic`(`statistic_id` int(11) NOT NULL AUTO_INCREMENT,`reason` varchar(255) DEFAULT NULL,`setup_time` time DEFAULT NULL,`durationSeconds` int(11) DEFAULT NULL,`durationNanoseconds` int(11) DEFAULT NULL,`recv_pkts` bigint(20) DEFAULT NULL,`recv_bytes` bigint(20) DEFAULT NULL,PRIMARY KEY(`statistic_id`))DEFAULT CHARSET=utf8";
            return statisticSql;
        }
        public String creatematchtable(){
            String matchSql;
            matchSql="create table `match`(`match_id` int(11) NOT NULL AUTO_INCREMENT,`IN_PORT` text DEFAULT NULL,`OUT_PORT` text DEFAULT NULL,`DL_SRC` char(255) DEFAULT NULL,`DL_DST` char(255) DEFAULT NULL,`DL_TYPE` smallint(6) DEFAULT NULL,`DL_VLAN` smallint(6) DEFAULT NULL,`DL_VLAN_PR` tinyint(4) DEFAULT NULL,`NW_SRC` varchar(255) DEFAULT NULL,`NW_DST` varchar(255) DEFAULT NULL,`NW_PROTO` int(4) DEFAULT NULL,`NW_TOS` tinyint(4) DEFAULT NULL,`TP_SRC` int(11) DEFAULT NULL,`TP_DST` int(11) DEFAULT NULL,PRIMARY KEY(`match_id`),INDEX (`NW_PROTO`),foreign key(NW_PROTO) references protocol(Protocol_id) on delete cascade on update cascade)DEFAULT CHARSET=utf8";
            return matchSql;
        }
        public String createflowtable(){
            String flowSql;
            flowSql="create table `flow`(`id` bigint(20) NOT NULL AUTO_INCREMENT,`flow_id` bigint(20),`node_id` varchar(30),`node_type` int(20),`entry_priority` smallint(11),`actions` int(225),`match_id` int(225),`statistic_id` int(11),`idleTimeout` datetime DEFAULT NULL,`hardTimeout` datetime DEFAULT NULL,`cookie` text  DEFAULT NULL, PRIMARY KEY(`id`),INDEX (`node_type`),FOREIGN KEY(node_type) references nodetype(type_id) on delete cascade on update cascade,INDEX (`actions`),FOREIGN KEY(actions) references action(action_id) on delete cascade on update cascade,INDEX (`statistic_id`),FOREIGN KEY(statistic_id) references statistic(statistic_id) on delete cascade on update cascade)DEFAULT CHARSET=utf8";
            return flowSql;
        }
        public String createflowkeytable(){
            String flowkeySql;
            flowkeySql="ALTER TABLE `flow` ADD CONSTRAINT match1 FOREIGN KEY (`match_id`) REFERENCES `match`(`match_id`) on delete CASCADE ON UPDATE CASCADE";
            return flowkeySql;
        }
}
