/*
GRANITE DATA SERVICES
Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

This file is part of Granite Data Services.

Granite Data Services is free software; you can redistribute it and/or modify
it under the terms of the GNU Library General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

Granite Data Services is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
for more details.

You should have received a copy of the GNU Library General Public License
along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.tide.collections {
	
	import mx.collections.*;
	
	
	CONFIG::flex40
	public class SortableAsyncListView {
	}		
	
	CONFIG::flex45
	public class SortableAsyncListView extends AsyncListView implements ICollectionView {
		
		public function contains(item:Object):Boolean {
			return ListCollectionView(list).contains(item);
		}
		
		public function createCursor():IViewCursor {
			return ListCollectionView(list).createCursor();
		}
		
		public function disableAutoUpdate():void {
			ListCollectionView(list).disableAutoUpdate();			
		}
		
		public function enableAutoUpdate():void {
			ListCollectionView(list).enableAutoUpdate();
		}
		
		public function get filterFunction():Function {
			return ListCollectionView(list).filterFunction;
		}
		
		public function set filterFunction(value:Function):void {
			ListCollectionView(list).filterFunction = value;
		}
		
		public function refresh():Boolean {
			return ListCollectionView(list).refresh();
		}
		
		public function get sort():ISort {
			return ListCollectionView(list).sort;
		}
		
		public function set sort(value:ISort):void {
			ListCollectionView(list).sort = value;
		}
	}
}