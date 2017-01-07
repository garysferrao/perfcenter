package perfcenter.baseclass;
import perfcenter.baseclass.enums.DeviceType;

public class DeviceCategory {
	public String name;
	public DeviceType type;
	public DeviceCategory(String _name, DeviceType _devType){
		this.name =  _name;
		this.type = _devType;
	}
	public DeviceCategory getCopy() {
		DeviceCategory dccpy = new DeviceCategory(name, type);
		return dccpy;
	}
}
