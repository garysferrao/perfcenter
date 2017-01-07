/*
 * Copyright (C) 2015-  by Varsha Apte - <varsha@cse.iitb.ac.in>, et al.
 * This file is distributed as part of PerfCenter
 *
 *  This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package perfcenter.baseclass;

import perfcenter.baseclass.DeviceCategory;


/**
 * This class defines virtual devices that would be deployed on Host.
 * 
 */
public class VirtualDevice extends Device {
	public VirtualDevice() {
		super();
	}

	public VirtualDevice(String _name) {
		super(_name);
	}
	
	public VirtualDevice(String _name, DeviceCategory _category) {
		super(_name, _category);
	}
	
	public VirtualDevice(String _name, String _catname) {
		super(_name, _catname);
	}
	
	public VirtualDevice(String _name, DeviceCategory _category, double _baselineSpeed){
		super(_name, _category, _baselineSpeed);
	}
	
	public VirtualDevice(String _name, String _catname, double _baselineSpeed){
		super(_name, _catname, _baselineSpeed);
	}
	
}