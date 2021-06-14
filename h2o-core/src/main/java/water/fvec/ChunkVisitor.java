package water.fvec;

import water.parser.BufferedString;
import water.util.PrettyPrint;

/**
 * Created by tomas on 3/8/17.
 * Base class for using visitor pattern with chunks.
 */
public abstract class ChunkVisitor {
  public boolean expandedVals() {
    return false;
  }

  public void addValue(BufferedString bs) {
    throw new UnsupportedOperationException();
  }

  public void addValue(long uuid_lo, long uuid_hi) {
    throw new UnsupportedOperationException();
  }

  public void addValue(int val) {
    throw new UnsupportedOperationException();
  }

  public void addValue(double val) {
    throw new UnsupportedOperationException();
  }

  public void addValue(long val) {
    throw new UnsupportedOperationException();
  }

  public void addValue(long m, int e) {
    addValue(PrettyPrint.pow10(m,e));
  }

  public void addZeros(int zeros) {
    throw new UnsupportedOperationException();
  }

  public void addNAs(int nas) {
    throw new UnsupportedOperationException();
  }

  /**
   * Visitor wrapper around NewChunk. Usefull for extracting rows from chunks.
   */
  public static final class NewChunkVisitor extends ChunkVisitor {
    final NewChunk _nc;
    public NewChunkVisitor(NewChunk nc){_nc = nc;}
    @Override
    public boolean expandedVals(){return true;}
    @Override
    public void addValue(BufferedString bs){_nc.addStr(bs);}
    @Override
    public void addValue(long uuid_lo, long uuid_hi){_nc.addUUID(uuid_lo,uuid_hi);}
    @Override
    public void addValue(int val) {_nc.addNum(val,0);}
    @Override
    public void addValue(long val) {_nc.addNum(val,0);}
    @Override
    public void addValue(long val, int exp) {_nc.addNum(val,exp);}
    @Override
    public void addValue(double val) {_nc.addNum(val);}
    @Override
    public void addZeros(int zeros) {_nc.addZeros(zeros);}
    @Override
    public void addNAs(int nas) {_nc.addNAs(nas);}
  }

  /**
   * Simple chunk visitor for extracting rows from chunks into a double array.
   */
  public static final class DoubleAryVisitor extends ChunkVisitor {
    public final double [] vals;
    private int _k = 0;
    private final double _na;
    DoubleAryVisitor(double [] vals){this(vals,Double.NaN);}
    DoubleAryVisitor(double [] vals, double NA){
      this.vals = vals; _na = NA;}
    @Override
    public void addValue(int val) {
      vals[_k++] = val;}
    @Override
    public void addValue(long val) {
      vals[_k++] = val;}
    @Override
    public void addValue(double val) {
      vals[_k++] = Double.isNaN(val)?_na:val;}
    @Override
    public void addZeros(int zeros) {
      int k = _k;
      int kmax = k +zeros;
      for(;k < kmax; k++) vals[k] = 0;
      _k = kmax;
    }
    @Override
    public void addNAs(int nas) {
      int k = _k;
      int kmax = k + nas;
      for(;k < kmax; k++) vals[k] = _na;
      _k = kmax;
    }
  }

  public static final class ShufflingDoubleAryVisitor extends ChunkVisitor {
    public final double [] vals;
    private final int[] _dest;
    private int _k = 0;
    ShufflingDoubleAryVisitor(double[] vals, int[] dest){
      this.vals = vals;
      _dest = dest;
    }
    @Override
    public void addValue(int val) {
      int d = _dest[_k++];
      if (d >= 0)
        vals[d] = val;
    }
    @Override
    public void addValue(long val) {
      int d = _dest[_k++];
      if (d >= 0)
        vals[d] = val;
    }
    @Override
    public void addValue(double val) {
      int d = _dest[_k++];
      if (d >= 0)
        vals[d] = val;
    }
    @Override
    public void addZeros(int zeros) {
      int k = _k;
      int kmax = k + zeros;
      for(;k < kmax; k++) {
        int d = _dest[k];
        if (d >= 0)
          vals[d] = 0;
      }
      _k = kmax;
    }
    @Override
    public void addNAs(int nas) {
      int k = _k;
      int kmax = k + nas;
      for(;k < kmax; k++) {
        int d = _dest[k];
        if (d >= 0)
          vals[d] = Double.NaN;
      }
      _k = kmax;
    }
  }


  /**
   * Simple chunk visitor for extracting rows from chunks into a sparse double array.
   */
  public static final class SparseDoubleAryVisitor extends ChunkVisitor {
    public final boolean naSparse;
    public final double [] vals;
    public final int [] ids;
    private int _sparseLen;
    private int _len;
    private final double _na;

    public int sparseLen(){return _sparseLen;}
    SparseDoubleAryVisitor(double [] vals, int [] ids){this(vals,ids,false,Double.NaN);}
    SparseDoubleAryVisitor(double [] vals, int [] ids, boolean naSparse){this(vals, ids, naSparse, Double.NaN);}
    SparseDoubleAryVisitor(double [] vals, int [] ids, boolean naSparse, double NA){this.vals = vals; this.ids = ids; _na = NA; this.naSparse = naSparse;}
    @Override
    public void addValue(int val) {ids[_sparseLen] = _len++; vals[_sparseLen++] = val;}
    @Override
    public void addValue(long val) {ids[_sparseLen] = _len++; vals[_sparseLen++] = val;}
    @Override
    public void addValue(double val) {ids[_sparseLen] = _len++; vals[_sparseLen++] = Double.isNaN(val)?_na:val;}
    @Override
    public void addZeros(int zeros) {
      if(naSparse) {
        int kmax = _sparseLen + zeros;
        for (int k = _sparseLen; k < kmax; k++) {
          ids[k] = _len++;
          vals[k] = 0;
        }
        _sparseLen = kmax;
      } else
        _len += zeros;
    }
    @Override
    public void addNAs(int nas) {
      if(!naSparse) {
        int kmax = _sparseLen + nas;
        for (int k = _sparseLen; k < kmax; k++) {
          ids[k] = _len++;
          vals[k] = _na;
        }
        _sparseLen = kmax;
      } else
        _len += nas;
    }
  }
  /**
   * Chunk visitor for combining values from chunk with values from a given double array
   */
  public static final class CombiningDoubleAryVisitor extends ChunkVisitor {
    public final double [] vals;
    private int _k = 0;
    private final double _na;
    public CombiningDoubleAryVisitor(double [] vals){this(vals,Double.NaN);}
    CombiningDoubleAryVisitor(double [] vals, double NA){
      this.vals = vals; _na = NA;}
    @Override
    public void addValue(int val) {
      vals[_k++] += val;}
    @Override
    public void addValue(long val) {
      vals[_k++] += val;}
    @Override
    public void addValue(double val) {
      if (Double.isNaN(val))
        vals[_k++] = _na;
      else
        vals[_k++] += val;}
    @Override
    public void addZeros(int zeros) {
      _k += zeros;
    }
    @Override
    public void addNAs(int nas) {
      int k = _k;
      int kmax = k + nas;
      for(;k < kmax; k++) vals[k] = _na;
      _k = kmax;
    }
    public void reset() {
      _k = 0;
    }
  }
  /**
   * Simple chunk visitor for extracting rows from chunks into a integer array.
   */
  public static final class IntAryVisitor extends ChunkVisitor {
    public final int [] vals;
    private int _k = 0;
    private final int _na;
    IntAryVisitor(int [] vals){this(vals,(int)C4Chunk._NA);}
    IntAryVisitor(int [] vals, int NA){this.vals = vals; _na = NA;}
    @Override
    public void addValue(int val) {vals[_k++] = val;}
    @Override
    public void addValue(long val) {
      if(Integer.MAX_VALUE < val || val < Integer.MIN_VALUE)
        throw new RuntimeException(val + " does not fit into int");
      vals[_k++] = (int)val;
    }
    @Override
    public void addValue(double val) {
      if (Double.isNaN(val)) {
        vals[_k++] = _na;
      } else {
        int i = (int) val;
        if (i != val)
          throw new RuntimeException(val + " does not fit into int");
        vals[_k++] = i;
      }
    }
    @Override
    public void addZeros(int zeros) {
      int k = _k;
      int kmax = k +zeros;
      for(;k < kmax; k++)vals[k] = 0;
      _k = kmax;
    }
    @Override
    public void addNAs(int nas) {
      int k = _k;
      int kmax = k + nas;
      for(;k < kmax; k++)vals[k] = _na;
      _k = kmax;
    }
  }

}
