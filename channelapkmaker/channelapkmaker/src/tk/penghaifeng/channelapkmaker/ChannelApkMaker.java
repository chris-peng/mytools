package tk.penghaifeng.channelapkmaker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class ChannelApkMaker {
	private static String cmd_apktool = "apktool";
	private static String cmd_sign = "sign";
	static {
		if (System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) {
			cmd_apktool += ".bat";
			cmd_sign += ".bat";
		}
	}
	private static final String CHANNEL_ELEMENT_NAME = "UMENG_CHANNEL";
	private static final String ANDROID_MANIFEST_FILE_ENCODING = "UTF-8";

	private String apkPath, channelListFilePath, outputDirPath, apkName,
			sourceTempDirPath;
	private String logPath;

	public ChannelApkMaker(String apkPath, String channelListFilePath,
			String outputDirPath) {
		this.apkPath = apkPath;
		this.channelListFilePath = channelListFilePath;
		this.outputDirPath = outputDirPath;
	}

	public boolean make() {
		File apkFile = new File(apkPath);
		File channelListFile = new File(channelListFilePath);
		File outputDir = new File(outputDirPath);

		if (!apkFile.exists()) {
			log("apk文件不存在");
			return false;
		}
		if (!channelListFile.exists()) {
			log("渠道列表文件不存在");
			return false;
		}
		outputDir.mkdirs();

		logPath = getAppPath(ChannelApkMaker.class) + File.separator + "cam."
				+ System.currentTimeMillis() + ".log";

		apkPath = apkFile.getAbsolutePath();
		channelListFilePath = channelListFile.getAbsolutePath();
		outputDirPath = outputDir.getAbsolutePath();

		int lastNamePos = apkPath.lastIndexOf(File.separatorChar);
		apkName = apkPath.substring(lastNamePos == -1 ? 0 : lastNamePos,
				apkPath.lastIndexOf('.'));
		String apkDirPath = apkPath.substring(0, lastNamePos);
		sourceTempDirPath = apkDirPath + File.separator + apkName
				+ System.currentTimeMillis();
		try {
			String cmd = cmd_apktool + " d -s -d -f " + apkPath + " "
					+ sourceTempDirPath;
			Process p = Runtime.getRuntime().exec(cmd);
			printCmdResult(p);
			p.waitFor();
			if(p.exitValue() != 0){
				throw new RuntimeException("命令" + cmd + "执行失败！");
			}
			generateAllApks(channelListFilePath, sourceTempDirPath,
					outputDirPath);
			clean();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return true;
	}

	private void generateAllApks(String channelListFilePath,
			String sourceTempDirPath, String outputDirPath)
			throws DocumentException, InterruptedException, IOException {
		File androidManifestFile = new File(sourceTempDirPath + File.separator
				+ "AndroidManifest.xml");
		SAXReader saxReader = new SAXReader();
		Document androidManifestDocument = saxReader.read(androidManifestFile);
		BufferedReader channelListFileReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(channelListFilePath)));
		String channelName = null;
		try {
			int i = 1;
			while ((channelName = channelListFileReader.readLine()) != null) {
				androidManifestDocument = modifyAndroidManifestDocument(
						androidManifestDocument, channelName);
				OutputFormat format = OutputFormat.createPrettyPrint();
				format.setEncoding(ANDROID_MANIFEST_FILE_ENCODING); // 指定XML编码
				XMLWriter writer = new XMLWriter(new FileWriter(
						androidManifestFile), format);
				writer.write(androidManifestDocument);
				writer.close();
				repackApk(channelName, sourceTempDirPath, outputDirPath);
				log(i + "、" + channelName
						+ "渠道包打包成功!!!!!!!!!!!!!");
				i++;
			}
		} finally {
			if (channelListFileReader != null) {
				try {
					channelListFileReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Document modifyAndroidManifestDocument(
			Document androidManifestDocument, String channelName) {
		Element applicationEl = androidManifestDocument.getRootElement()
				.element("application");
		if (applicationEl != null) {
			Element channelElement = null;
			Iterator<Element> iterator = applicationEl
					.elementIterator("meta-data");
			while (iterator.hasNext()) {
				Element el = iterator.next();
				Attribute attr = el.attribute("android:name");
				if (attr == null) {
					attr = el.attribute("name");
				}
				if (attr != null && CHANNEL_ELEMENT_NAME.equals(attr.getText())) {
					channelElement = el;
					break;
				}
			}
			if (channelElement != null) {
				// 删除渠道节点
				applicationEl.remove(channelElement);
			}
			// 新建并添加渠道节点
			channelElement = applicationEl.addElement("meta-data");
			channelElement.addAttribute("android:name", CHANNEL_ELEMENT_NAME);
			channelElement.addAttribute("android:value", channelName);
		}
		return androidManifestDocument;
	}

	private void repackApk(String channelName, String sourceTempDirPath,
			String outputDirPath) throws IOException, InterruptedException {
		// 打包
		String cmd = cmd_apktool + " b -d " + sourceTempDirPath;
		Process p = Runtime.getRuntime().exec(cmd);
		printCmdResult(p);
		p.waitFor();
		if(p.exitValue() != 0){
			throw new RuntimeException("命令" + cmd + "执行失败！");
		}
		// 签名
		cmd = cmd_sign + " " + sourceTempDirPath + File.separator
				+ "dist" + File.separator + apkName + ".apk "
				+ outputDirPath + File.separator + apkName + "_"
				+ channelName + ".apk";
		p = Runtime.getRuntime()
				.exec(cmd);
		printCmdResult(p);
		p.waitFor();
		if(p.exitValue() != 0){
			throw new RuntimeException("命令" + cmd + "执行失败！");
		}
	}

	private void clean() {
		log("清除临时文件...");
		File sourceTempDir = new File(sourceTempDirPath);
		deleteDir(sourceTempDir);
	}

	private void printCmdResult(Process p) {
		BufferedReader pbr = null;
		BufferedReader pebr = null;
		pbr = new BufferedReader(new InputStreamReader(p.getInputStream()));
		pebr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line = null;
		String errorLine = null;
		try {
			while ((errorLine = pebr.readLine()) != null) {
				System.out.println(errorLine);
				log(errorLine);
			}
			while ((line = pbr.readLine()) != null) {
				System.out.println(line);
				log(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pbr != null) {
				try {
					pbr.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (pebr != null) {
				try {
					pebr.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void log(String line){
		System.out.println(line);
		FileWriter logFileWriter = null;
		try {
			logFileWriter = new FileWriter(logPath, true);
			PrintWriter pw = new PrintWriter(logFileWriter);
			pw.println((new Date().toLocaleString()) + "::" + line);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (logFileWriter != null) {
				try {
					logFileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * -----------------------------------------------------------------------
	 * getAppPath需要一个当前程序使用的Java类的class属性参数，它可以返回打包过的
	 * Java可执行文件（jar，war）所处的系统目录名或非打包Java程序所处的目录
	 *
	 * @param cls为Class类型
	 * @return 返回值为该类所在的Java程序运行的目录
	 *         ----------------------------------------------
	 *         ---------------------------
	 */
	public static String getAppPath(Class cls) {
		// 检查用户传入的参数是否为空
		if (cls == null)
			throw new java.lang.IllegalArgumentException("参数不能为空！");
		ClassLoader loader = cls.getClassLoader();
		// 获得类的全名，包括包名
		String clsName = cls.getName() + ".class";
		// 获得传入参数所在的包
		Package pack = cls.getPackage();
		String path = "";
		// 如果不是匿名包，将包名转化为路径
		if (pack != null) {
			String packName = pack.getName();
			// 此处简单判定是否是Java基础类库，防止用户传入JDK内置的类库
			if (packName.startsWith("java.") || packName.startsWith("javax."))
				throw new java.lang.IllegalArgumentException("不要传送系统类！");
			// 在类的名称中，去掉包名的部分，获得类的文件名
			clsName = clsName.substring(packName.length() + 1);
			// 判定包名是否是简单包名，如果是，则直接将包名转换为路径，
			if (packName.indexOf(".") < 0)
				path = packName + "/";
			else {// 否则按照包名的组成部分，将包名转换为路径
				int start = 0, end = 0;
				end = packName.indexOf(".");
				while (end != -1) {
					path = path + packName.substring(start, end) + "/";
					start = end + 1;
					end = packName.indexOf(".", start);
				}
				path = path + packName.substring(start) + "/";
			}
		}
		// 调用ClassLoader的getResource方法，传入包含路径信息的类文件名
		java.net.URL url = loader.getResource(path + clsName);
		// 从URL对象中获取路径信息
		String realPath = url.getPath();
		// 去掉路径信息中的协议名"file:"
		int pos = realPath.indexOf("file:");
		if (pos > -1)
			realPath = realPath.substring(pos + 5);
		// 去掉路径信息最后包含类文件信息的部分，得到类所在的路径
		pos = realPath.indexOf(path + clsName);
		realPath = realPath.substring(0, pos - 1);
		// 如果类文件被打包到JAR等文件中时，去掉对应的JAR等打包文件名
		if (realPath.endsWith("!"))
			realPath = realPath.substring(0, realPath.lastIndexOf("/"));
		/*------------------------------------------------------------ 
		 ClassLoader的getResource方法使用了utf-8对路径信息进行了编码，当路径 
		  中存在中文和空格时，他会对这些字符进行转换，这样，得到的往往不是我们想要 
		  的真实路径，在此，调用了URLDecoder的decode方法进行解码，以便得到原始的 
		  中文及空格路径 
		-------------------------------------------------------------*/
		try {
			realPath = java.net.URLDecoder.decode(realPath, "utf-8");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return realPath;
	}

	private static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			// 递归删除目录中的子目录下
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// 目录此时为空，可以删除
		return dir.delete();
	}
}
