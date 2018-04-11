import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.omg.CORBA.PRIVATE_MEMBER;

import com.sun.beans.decoder.ValueObject;
import com.sun.jmx.snmp.SnmpUnknownSubSystemException;
import com.sun.org.apache.bcel.internal.generic.IDIV;
import com.sun.security.auth.NTDomainPrincipal;

import javafx.stage.FileChooser;


public class CCacheSim extends JFrame implements ActionListener{

	private JPanel panelTop, panelLeft, panelRight, panelBottom;
	private JButton execStepBtn, execAllBtn, fileBotton,resetBtn;
	private JComboBox csBox, bsBox, wayBox, replaceBox, prefetchBox, writeBox, allocBox;
	private JFileChooser fileChoose;
	
	private JLabel labelTop,labelLeft,rightLabel,bottomLabel,fileLabel,fileAddrBtn, stepLabel1, stepLabel2,
		    csLabel, bsLabel, wayLabel, replaceLabel, prefetchLabel, writeLabel, allocLabel,resetLabel;
	//自己添加的部分信息的label
	
	private JLabel visitTypelb, addrlb,blockNumlb,blockoffsetld,indexlb,hitsituationlb;
	
	private JLabel results[][];


    //参数定义
	private String cachesize[] = { "2KB", "8KB", "32KB", "128KB", "512KB", "2MB" };
	private String blocksize[] = { "16B", "32B", "64B", "128B", "256B" };
	private String way[] = { "直接映象", "2路", "4路", "8路", "16路", "32路" };
	private String replace[] = { "LRU", "FIFO", "RAND" };
	private String pref[] = { "不预取", "不命中预取" };
	private String write[] = { "写回法", "写直达法" };
	private String alloc[] = { "按写分配", "不按写分配" };
	private String typename[] = { "读数据", "写数据", "读指令" };
	private String hitname[] = {"不命中", "命中" };
	
	//右侧结果显示
	private String rightLable[]={"访问总次数：","读指令次数：","读数据次数：","写数据次数："};
	
	
	
	//打开文件
	private File file;
	
	//分别表示左侧几个下拉框所选择的第几项，索引从 0 开始
	private int csIndex, bsIndex, wayIndex, replaceIndex, prefetchIndex, writeIndex, allocIndex;
	
	//其它变量定义
	Random random = new Random();
	static boolean[] dirty;
	static boolean[][] valid;
	static int[][] tag;
	
	ArrayList<String> InstStream = new ArrayList<String>();
	int InstIndex = 0;	//索引文件流指令
	int InstNum = 0;	//记录总指令
	
	private int csnum[] = {2048,8192,32768,131072,524288,2097152};
	private int bsnum[] = {16,32,64,128,256};
	private int waynum[] = {1,2,4,8,16,32};
	
	//定义输出结果变量
	int visitMemNum = 0;
	int ldInst = 0;
	int wtData = 0;
	int ldData = 0;
	
	int readDataMissNum = 0;
	int writeDataMissNum = 0;
	int readInstMissNum = 0;
	int wholeMissNum = 0;
	
	float readDataMissRate = 0;
	float readInstMissRate = 0;
	float  writeDataMissRate = 0;
	float wholeMissRate = 0;
	
	
	int missNum;
	float missRate;
	BufferedReader reader = null;
	
	int cs;		//actual cachesize
	int bs;		//actual blocksize
	int wayy;	//actual waynum
	
	int wayynum;	
	int blockNum;	
	int groupNum;
	int offsetlen;	//位数
	int indexlen;
	int taglen;
	
	int[][] queue;
	int[][]RU;
	
	int addrTag;	//tag号
	int addrIndex;	//组号
	
	
	//建立对应组相应变量的引用
	boolean []gvalid;
	int[] gRU;
	int[] gtag;
	int[] gqueue;
	
	
	
	//...
	
	/*
	 * 构造函数，绘制模拟器面板
	 */
	public CCacheSim(){
		super("Cache Simulator");
		draw();
	}
	
	
	//响应事件，共有三种事件：
	//   1. 执行到底事件
	//   2. 单步执行事件
	//   3. 文件选择事件
	public void actionPerformed(ActionEvent e){
		fileChoose = new JFileChooser();		
		if (e.getSource() == execAllBtn) {
			simExecAll();
		}
		if (e.getSource() == execStepBtn) {
			simExecStep();
		}
		if (e.getSource() == fileBotton){
			int fileOver = fileChoose.showOpenDialog(null);
			if (fileOver == 0) {
				   String path = fileChoose.getSelectedFile().getAbsolutePath();
				   fileAddrBtn.setText(path);
				   file = new File(path);
				   readFile();
				   initCache();
			}
		}
		if(e.getSource() == resetBtn){
			fileAddrBtn.setText("");
			ClearValue();
			freshValue();
			visitTypelb.setText("");
			addrlb.setText("");
			blockNumlb.setText("");
			blockoffsetld.setText("");;
			indexlb.setText("");
			hitsituationlb.setText("");
		}
	}
	
	void ClearValue(){
		visitMemNum = 0;
		ldInst = 0;
		wtData = 0;
		ldData = 0;
		
		readDataMissNum = 0;
		writeDataMissNum = 0;
		readInstMissNum = 0;
		wholeMissNum = 0;
		
		readDataMissRate = 0;
		readInstMissRate = 0;
		writeDataMissRate = 0;
		wholeMissRate = 0;
		missNum = 0;
		missRate = 0;
		
		//模拟器部分复位
		valid = new boolean [groupNum][wayy];
		dirty = new boolean [groupNum];
		tag = new int [groupNum][wayy];
		RU = new int [groupNum][wayy];
		queue = new int [groupNum][wayy];
		for(int i = 0; i < groupNum; ++i){
			for(int j = 0; j < wayy;++j){
				queue[i][j] = j;
			}
		}
		
	}
	
	/*
	 * 初始化 Cache 模拟器
	 */
	public void initCache() {
		cs = csnum[csIndex];
		bs = bsnum[bsIndex];
		wayy = waynum[wayIndex];
		
		ClearValue();
		blockNum = csnum[csIndex] / bsnum[bsIndex];
		groupNum = blockNum / waynum[wayIndex];
		
		offsetlen = bsIndex+4;
		indexlen = wayIndex;
	//	taglen = 32 - offsetlen - indexlen;

		//模拟器部分初始化
		valid = new boolean [groupNum][wayy];
		dirty = new boolean [groupNum];
		tag = new int [groupNum][wayy];
		RU = new int [groupNum][wayy];
		queue = new int [groupNum][wayy];
		for(int i = 0; i < groupNum; ++i){
			for(int j = 0; j < wayy;++j){
				queue[i][j] = j;
			}
		}
		
		visitMemNum = 0;
		missNum = 0;
		missRate = 0;
	}
	
	/*
	 * 将指令和数据流从文件中读入
	 */
	public void readFile() {
		try{
			InstStream.clear();
			InstNum = 0;
			InstIndex = 0;
			reader = new BufferedReader(new FileReader(file));
			String temp;
			while((temp = reader.readLine()) != null && !"".equals(temp)){
				InstNum++;
				InstStream.add(temp);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		finally {
			if(reader!=null){
				try{
					reader.close();
				}catch(IOException e1){}
			}
		}
	}
	
	/*
	 * 模拟单步执行
	 */
	
	private int LRU(char type){
		boolean hit = false;
		int i;
		for(i = 0; i < wayy && gRU[i] != 0; ++i){
			if(gtag[gRU[i] - 1] == addrTag){
				hit = true;//hit 更新RU,调整i到队首
				int temp = gRU[i];
				for(int j = i; j > 0; --j){
					gRU[j] = gRU[j - 1];
				}
				gRU[0] = temp; 
				break;
			}
		}
		if(!hit){
			addMissNum(type);
			missNum++;
			if(i != wayy){	//队列未满，直接在最后插入
				gRU[i] = i+1;	//第i+1块插入队列
				gvalid[gRU[i] - 1] = true;
				gtag[gRU[i] - 1] = addrTag;
			}
			else{	//队列已满，删除队尾插入队首
				int temp = gRU[wayy - 1];
				for(int j = wayy - 1;j > 0; --j){
					gRU[j] = gRU[j - 1];
				}
				gRU[0] = temp;
				gtag[temp - 1] = addrTag;
				gvalid[temp - 1] = true;
			}
			return 0;
		}
		else return 1;
	}
	
	private void addMissNum(char type){
		switch (type) {
		case '0':
			readDataMissNum++;
			break;
		case '1':
			writeDataMissNum++;
			break;
		case '2':
			readInstMissNum++;
			break;
		default:
			System.out.println("file stream undefined!!");
			break;
		}
	}
	
	private int FIFO(char type){		
		boolean hit = false;
		int i = 0;
		for(i = 0; i < wayy && gRU[i] != 0; ++i){
			if(gvalid[i] == false) continue;
			else if(gtag[i] == addrTag){	//hit
				hit = true;
				break;
			}
		}
		if(hit == false){
			missNum ++;
			addMissNum(type);
			if(i != wayy){	//队列未满，直接在最后插入
				gRU[i] = i+1;	//第i+1块插入队列
				gvalid[gRU[i] - 1] = true;
				gtag[gRU[i] - 1] = addrTag;
			}
			else{	//队列已满，删除队尾插入队首
				int temp = gRU[wayy - 1];
				for(int j = wayy - 1;j > 0; --j){
					gRU[j] = gRU[j - 1];
				}
				gRU[0] = temp;
				gtag[temp - 1] = addrTag;
				gvalid[temp - 1] = true;
			}
			return 0;
		}
		else return 1;
	}
	
	private int rdm(char type){
		boolean hit = false;
		for(int i = 0; i < wayy; ++i){
			if(gvalid[i] == false) continue;
			else if(gtag[i] == addrTag){
				hit = true;
				break;
			}
		}
		if(hit == false){
			missNum++;
			addMissNum(type);
			int temp = random.nextInt(wayy);
			gtag[temp] = addrTag;
			gvalid[temp] = true;
			return 0;
		}
		else return 1;
	}
	
	
	
	
	public void simExecStep() {
		char type = '3';
		int address = 0;
		try{
			String tempString = InstStream.get(InstIndex);
			++InstIndex;
			if(tempString!= null){
				System.out.println(tempString);
				visitMemNum++;
				String strs[] = tempString.split(" ");
				address = Integer.valueOf(strs[1],16);
				type = strs[0].charAt(0);
			}
			else{
				reader.close();
			}
		}catch (IOException e) {
			System.out.println("file read errorsb");
		}
		//根据读到的地址确定一些量
		addrTag = (address >>> offsetlen) / groupNum;	//tag号
		addrIndex = (address >>> offsetlen) % groupNum ;	//组号
		
		
		//建立对应组相应变量的引用
		gvalid = valid[addrIndex];
		gRU = RU[addrIndex];
		gtag = tag[addrIndex];
		
		
		switch(type){
		case'0':
			ldData++;
			break;
		case '1':
			wtData++;
			break;
		case '2':
			ldInst++;
			break;
		}
		int hitsit = 0;
		switch(replaceIndex){
		case 0:
			hitsit = LRU(type);
			break;
		case 1:
			hitsit = FIFO(type);
			break;
		case 2:
			hitsit = rdm(type);
			break;
		default:
			System.out.println("error: Undefined filestream");
		}
		
		resultCompute();
		freshValue();
		
		String visittype[] = {"读数据","写数据","读指令"};
		String whetherhit[] = {"未命中","已命中"};
		visitTypelb.setText("访问类型: " + visittype[type - '0']);
		addrlb.setText("地址: " + address);
		blockNumlb.setText("块号: " + Integer.toString(address >>> offsetlen));
		blockoffsetld.setText("块内地址: " + Integer.toString(address % (1 << offsetlen)));;
		indexlb.setText("索引: " + addrIndex);
		hitsituationlb.setText("命中情况: " + whetherhit[hitsit]);
	}
	void freshValue(){
		String[] s = {"不命中次数:\t","不命中率:\t"};
		NumberFormat nt = NumberFormat.getPercentInstance();
		nt.setMinimumFractionDigits(2);
		
		results[0][0].setText(rightLable[0] + visitMemNum);
		results[0][1].setText(s[0] +missNum);
		results[0][2].setText(s[1] + nt.format(missRate));
		results[1][0].setText(rightLable[1] + ldInst);
		results[1][1].setText(s[0] +readInstMissNum);
		results[1][2].setText(s[1] + nt.format(readInstMissRate));
		results[2][0].setText(rightLable[2] + ldData);
		results[2][1].setText(s[0] + readDataMissNum);
		results[2][2].setText(s[1] + nt.format(readDataMissRate));
		results[3][0].setText(rightLable[3] + wtData);
		results[3][1].setText(s[0] + writeDataMissNum);
		results[3][2].setText(s[1]+ nt.format(writeDataMissRate));
		
	}
	
	
	/*
	 * 模拟执行到底
	 */
	public void simExecAll() {
		while(InstIndex != InstNum){
			if(InstIndex == 1078)
				System.out.println("sb!!");
			simExecStep();
		}
		resultCompute();
	}

	
	private void resultCompute(){
		if(ldData == 0) readDataMissRate = 0;
		else readDataMissRate = (float)readDataMissNum / ldData;
		if(ldInst == 0)	readInstMissRate = 0;
		else readInstMissRate = (float)readInstMissNum / ldInst;
		if(wtData == 0) writeDataMissRate = 0;
		else writeDataMissRate = (float)writeDataMissNum / wtData;
		if(visitMemNum == 0) missRate = 0;
		else missRate = (float)missNum / visitMemNum;
	}
	
	public static void main(String[] args) {
		new CCacheSim();
	}
	
	/**
	 * 绘制 Cache 模拟器图形化界面
	 * 无需做修改
	 */
	public void draw() {
		//模拟器绘制面板
		setLayout(new BorderLayout(5,5));
		panelTop = new JPanel();
		panelLeft = new JPanel();
		panelRight = new JPanel();
		panelBottom = new JPanel();
		panelTop.setPreferredSize(new Dimension(800, 50));
		panelLeft.setPreferredSize(new Dimension(300, 450));
		panelRight.setPreferredSize(new Dimension(500, 450));
		panelBottom.setPreferredSize(new Dimension(800, 100));
		panelTop.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelLeft.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelRight.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelBottom.setBorder(new EtchedBorder(EtchedBorder.RAISED));

		//*****************************顶部面板绘制*****************************************//
		labelTop = new JLabel("Cache Simulator");
		labelTop.setAlignmentX(CENTER_ALIGNMENT);
		panelTop.add(labelTop);

		
		//*****************************左侧面板绘制*****************************************//
		labelLeft = new JLabel("Cache 参数设置");
		labelLeft.setPreferredSize(new Dimension(300, 40));
		
		resetBtn = new JButton("复位");
		resetBtn.setLocation(10,30);
		resetBtn.addActionListener(this);
		
		//cache 大小设置
		csLabel = new JLabel("总大小");
		csLabel.setPreferredSize(new Dimension(120, 30));
		csBox = new JComboBox(cachesize);
		csBox.setPreferredSize(new Dimension(160, 30));
		csBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				csIndex = csBox.getSelectedIndex();
			}
		});
		
		//cache 块大小设置
		bsLabel = new JLabel("块大小");
		bsLabel.setPreferredSize(new Dimension(120, 30));
		bsBox = new JComboBox(blocksize);
		bsBox.setPreferredSize(new Dimension(160, 30));
		bsBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				bsIndex = bsBox.getSelectedIndex();
			}
		});
		
		//相连度设置
		wayLabel = new JLabel("相联度");
		wayLabel.setPreferredSize(new Dimension(120, 30));
		wayBox = new JComboBox(way);
		wayBox.setPreferredSize(new Dimension(160, 30));
		wayBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				wayIndex = wayBox.getSelectedIndex();
			}
		});
		
		//替换策略设置
		replaceLabel = new JLabel("替换策略");
		replaceLabel.setPreferredSize(new Dimension(120, 30));
		replaceBox = new JComboBox(replace);
		replaceBox.setPreferredSize(new Dimension(160, 30));
		replaceBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				replaceIndex = replaceBox.getSelectedIndex();
			}
		});
		
		//欲取策略设置
		prefetchLabel = new JLabel("预取策略");
		prefetchLabel.setPreferredSize(new Dimension(120, 30));
		prefetchBox = new JComboBox(pref);
		prefetchBox.setPreferredSize(new Dimension(160, 30));
		prefetchBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				prefetchIndex = prefetchBox.getSelectedIndex();
			}
		});
		
		//写策略设置
		writeLabel = new JLabel("写策略");
		writeLabel.setPreferredSize(new Dimension(120, 30));
		writeBox = new JComboBox(write);
		writeBox.setPreferredSize(new Dimension(160, 30));
		writeBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				writeIndex = writeBox.getSelectedIndex();
			}
		});
		
		//调块策略
		allocLabel = new JLabel("写不命中调块策略");
		allocLabel.setPreferredSize(new Dimension(120, 30));
		allocBox = new JComboBox(alloc);
		allocBox.setPreferredSize(new Dimension(160, 30));
		allocBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				allocIndex = allocBox.getSelectedIndex();
			}
		});
		
		//选择指令流文件
		fileLabel = new JLabel("选择指令流文件");
		fileLabel.setPreferredSize(new Dimension(120, 30));
		fileAddrBtn = new JLabel();
		fileAddrBtn.setPreferredSize(new Dimension(210,30));
		fileAddrBtn.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		fileBotton = new JButton("浏览");
		fileBotton.setPreferredSize(new Dimension(70,30));
		fileBotton.addActionListener(this);
		
		panelLeft.add(labelLeft);
		panelLeft.add(csLabel);
		panelLeft.add(csBox);
		panelLeft.add(bsLabel);
		panelLeft.add(bsBox);
		panelLeft.add(wayLabel);
		panelLeft.add(wayBox);
		panelLeft.add(replaceLabel);
		panelLeft.add(replaceBox);
		panelLeft.add(prefetchLabel);
		panelLeft.add(prefetchBox);
		panelLeft.add(writeLabel);
		panelLeft.add(writeBox);
		panelLeft.add(allocLabel);
		panelLeft.add(allocBox);
		panelLeft.add(fileLabel);
		panelLeft.add(fileAddrBtn);
		panelLeft.add(fileBotton);
		panelLeft.add(resetBtn);
		
		//*****************************右侧面板绘制*****************************************//
		//模拟结果展示区域
		rightLabel = new JLabel("模拟结果");
		rightLabel.setPreferredSize(new Dimension(500, 40));
		results = new JLabel[4][3];
	
		String[] s = {"不命中次数:\t","不命中率:\t"};
		results[0][0] = new JLabel(rightLable[0] + ":\t" + visitMemNum);
		results[0][1] = new JLabel(s[0] + missNum);
		results[0][2] = new JLabel(s[1] + missRate);
		results[1][0] = new JLabel(rightLable[1] + ldInst);
		results[1][1] = new JLabel(s[0] + readInstMissNum);
		results[1][2] = new JLabel(s[1] + readInstMissRate);
		results[2][0] = new JLabel(rightLable[2] + ldData);
		results[2][1] = new JLabel(s[0] + readDataMissNum);
		results[2][2] = new JLabel(s[1] + readDataMissRate);
		results[3][0] = new JLabel(rightLable[3] + wtData);
		results[3][1] = new JLabel(s[0] + writeDataMissNum);
		results[3][2] = new JLabel(s[1]+ writeDataMissRate);
		
		stepLabel1 = new JLabel();
		stepLabel1.setVisible(false);
		stepLabel1.setPreferredSize(new Dimension(500, 40));
		stepLabel2 = new JLabel();
		stepLabel2.setVisible(false);
		stepLabel2.setPreferredSize(new Dimension(500, 40));
		
		
		panelRight.add(rightLabel);
		for (int i=0; i<4; i++) {
			for(int j = 0; j < 3; ++j){
				results[i][j].setPreferredSize(new Dimension(120, 30));
				panelRight.add(results[i][j]);
			}
		}
		visitTypelb = new JLabel();
		addrlb = new JLabel();
		blockNumlb = new JLabel();
		blockoffsetld = new JLabel();
		indexlb = new JLabel();
		hitsituationlb = new JLabel();
		
		panelRight.add(stepLabel1);
		panelRight.add(stepLabel2);
		panelRight.add(visitTypelb);
		visitTypelb.setPreferredSize(new Dimension(500, 40));
		panelRight.add(addrlb);
	//	addrlb.setPreferredSize(new Dimension(500, 40));
		panelRight.add(blockNumlb);
	//	blockNumlb.setPreferredSize(new Dimension(500, 40));
		panelRight.add(blockoffsetld);
	//	blockoffsetld.setPreferredSize(new Dimension(500, 40));
		panelRight.add(indexlb);
	//	indexlb.setPreferredSize(new Dimension(500, 40));
		panelRight.add(hitsituationlb);
	//	hitsituationlb.setPreferredSize(new Dimension(500, 40));


		//*****************************底部面板绘制*****************************************//
		
		bottomLabel = new JLabel("执行控制");
		bottomLabel.setPreferredSize(new Dimension(800, 30));
		execStepBtn = new JButton("步进");
		execStepBtn.setLocation(100, 30);
		execStepBtn.addActionListener(this);
		execAllBtn = new JButton("执行到底");
		execAllBtn.setLocation(300, 30);
		execAllBtn.addActionListener(this);
		
		panelBottom.add(bottomLabel);
		panelBottom.add(execStepBtn);
		panelBottom.add(execAllBtn);

		add("North", panelTop);
		add("West", panelLeft);
		add("Center", panelRight);
		add("South", panelBottom);
		setSize(820, 620);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
