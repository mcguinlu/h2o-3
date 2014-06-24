significantDigitsBeforeDecimal = (value) -> 1 + Math.floor Math.log(value) / Math.LN10

formatToSignificantDigits = (digits, value) ->
  if value is 0
    0
  else
    sd = significantDigitsBeforeDecimal value
    if sd >= digits
      value.toFixed 0
    else
      magnitude = Math.pow 10, digits - sd
      Math.round(value * magnitude) / magnitude

formatTime = d3.time.format '%Y-%m-%d %H:%M:%S'
formatDateTime = (time) -> if time then formatTime new Date time else '-'

formatReal = do ->
  __formatFunctions = {}
  getFormatFunction = (precision) ->
    if precision is -1
      identity
    else
      __formatFunctions[precision] or __formatFunctions[precision] = d3.format ".#{precision}f"

  (precision, value) ->
    (getFormatFunction precision) value

Steam.FrameView = (_, _frame) ->
  createSummaryRow = (attribute, columns) ->
    header: attribute
    cells: map columns, (column) ->
      switch column.type
        when 'uuid'
          switch attribute
            when 'min', 'max', 'mean', 'sigma', 'cardinality'
              '-'
            else
              column[attribute]
        when 'enum'
          switch attribute
            when 'min', 'max', 'mean', 'sigma'
              '-'
            when 'cardinality'
              column.domain.length
            else
              column[attribute]
        when 'time'
          switch attribute
            when 'min', 'max', 'mean'
              formatDateTime column[attribute]
            when 'sigma'
              formatToSignificantDigits 6, column[attribute]
            when 'cardinality'
              '-'
            else
              column[attribute]
        when 'real'
          switch attribute
            when 'cardinality'
              '-'
            when 'min', 'max', 'mean'
              formatReal column.precision, column[attribute]
            when 'sigma'
              formatToSignificantDigits 6, column[attribute]
            else
              column[attribute]
        else # int
          switch attribute
            when 'cardinality'
              '-'
            when 'min', 'max', 'mean', 'sigma'
              formatToSignificantDigits 6, column[attribute]
            else
              column[attribute]

  createDataRow = (offset, index, columns) ->
    header: "Row #{offset + index}"
    cells: map columns, (column) ->
      switch column.type
        when 'uuid'
          column.str_data[index] or '-'
        when 'enum'
          column.domain[column.data[index]]
        when 'time'
          formatDateTime column.data[index]
        else
          value = column.data[index]
          if value is 'NaN'
            '-'
          else
            if column.type is 'real'
              formatReal column.precision, value
            else
              value

  createSummaryRows = (columns) ->
    attributes = words 'type min max mean sigma cardinality'
    push attributes, 'missing' if some columns, (column) -> column.missing > 0
    rows = []
    for attribute in attributes
      rows.push createSummaryRow attribute, columns
    rows

  createDataRows = (offset, rowCount, columns) ->
    rows = []
    for index in [0 ... rowCount]
      rows.push createDataRow offset, index, columns
    rows

  createFrameTable = (offset, rowCount, columns) ->
    header: createSummaryRow 'label', columns
    summaryRows: createSummaryRows columns
    dataRows: createDataRows offset, rowCount, columns

  data: _frame
  key: _frame.key.name
  timestamp: _frame.creation_epoch_time_millis
  title: _frame.key.name
  columns: _frame.column_names
  table: createFrameTable _frame.off, _frame.len, _frame.columns
  isRawFrame: _frame.is_raw_frame
  parseUrl: "/2/Parse2.query?source_key=#{encodeURIComponent _frame.key}"
  dispose: ->
  template: 'frame-view'

