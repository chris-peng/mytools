package tk.penghaifeng.channelapkmaker;

public class Main {
	
	public static void main(String[] args){
		System.out.println("渠道包打包程序，请先安装配置好java运行环境和apktool工具！");
		System.out.println("渠道列表文件一行一个渠道，输出目录建议使用新目录");
		System.out.println();
		if(args.length < 3){
			System.out.println("使用方法：java -jar xx.jar apk包路径 渠道列表文件 输出目录");
			return;
		}
		String apkPath = args[0];
		String channelListFilePath = args[1];
		String outputDirPath = args[2];
		
		ChannelApkMaker cam = new ChannelApkMaker(apkPath, channelListFilePath, outputDirPath);
		if(cam.make()){
			System.out.println("所有渠道包打包成功");
		}else{
			System.out.println("打包失败");
		}
	}
}
