/**
 * Created to limit the boxing and unboxing of primitive data types
 */
public class IntArrayList{

	private static final int DEFAULT_SIZE = 8;
	private int[] elementData;
	private int size;

	public IntArrayList(){
		this.size = 0;
		this.elementData = new int[DEFAULT_SIZE];
	}
	public IntArrayList(int CAPACITY){
		this.size = 0;
		this.elementData = new int[CAPACITY];
	}

	public void add(int val){
		if (size == elementData.length){
			int[] newData = new int[elementData.length + (elementData.length >> 1)];
			System.arraycopy(elementData, 0, newData, 0, elementData.length);
			elementData = newData;
		}
		elementData[size++] = val;
	}
	public void remove(int idx){
		if (idx < 0 || idx >= size) throw new IndexOutOfBoundsException(idx);
		System.arraycopy(elementData, idx+1, elementData, idx, (--size)-idx);
	}

	public int size(){
		return this.size;
	}
	public boolean isEmpty(){
		return this.size == 0;
	}
	public void clear(){
		this.size = 0;
		this.elementData = new int[DEFAULT_SIZE];
	}

	public int get(int idx){
		if (idx < 0 || idx >= size) throw new IndexOutOfBoundsException(idx);
		return elementData[idx];
	}
	public void set(int idx, int val){
		if (idx < 0 || idx >= size) throw new IndexOutOfBoundsException(idx);
		elementData[idx] = val;
	}

	public int indexOf(int val){
		for (int i = 0; i < size; i++){
			if (elementData[i] == val){
				return i;
			}
		}
		return -1;
	}
	public boolean contains(int val){
		return indexOf(val) >= 0;
	}

	public int[] toArray(){
		int[] output = new int[size];
		System.arraycopy(elementData, 0, output, 0, size);
		return output;
	}

	@Override
	public int hashCode(){
		int hashCode = 1;
		for (int i = 0; i < size; i++){
			hashCode = 31 * hashCode + elementData[i];
		}
		return hashCode;
	}
	@Override
	public boolean equals(Object o){
		if (o == this) return true;
		if (o instanceof IntArrayList intList){
			if (intList.size != size) return false;
			for (int i = 0; i < size; i++){
				if (elementData[i] != intList.elementData[i]) return false;
			}
		} else {
			return false;
		}
		return true;
	}
	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append('[');
		for (int i = 0; i < size; i++){
			stringBuilder.append(elementData[i]);
			if (i != size - 1){
				stringBuilder.append(',').append(' ');
			}
		}
		stringBuilder.append(']');
		return stringBuilder.toString();
	}
}
