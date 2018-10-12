package cn.hz.common;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HttpParseUtil {
	
//	private HttpMethod method;
//	private String	uri;
//	private HttpVersion httpVersion;
//	private HttpHeaders httpHeaders;
//
//	private Map<String, String> reqUriParams = null;
//	private Map<String, String> reqPostQueryParams = null;
//	private JSONObject reqPostJsonObj = null;

	/**
	 *
	 * @param uri: not prefixed with a '?' or a string ended with a '?'
	 * @return
	 */
	public static Map<String, String[]> getRequestParamsFromUri(String uri) {
		return getQueryParams("?" + uri);
	}

	/**
	 *
	 * @param bytes: bytes from http body
	 * @return
	 */
	public static Map<String, String[]> getRequestParamsFromPostBytes(byte[] bytes) {

		return getQueryParams("?" + new String(bytes, CharsetUtil.UTF_8));
	}

	/**
	 *
	 * @param content: ByteBuf from http body
	 * @return
	 */
	public static Map<String, String[]> getRequestParamsFromPostBytebuf(ByteBuf content) {

		return getQueryParams("?" + content.toString(CharsetUtil.UTF_8));
	}

	/**
	 *
	 * Reference: chapter3.1 in servlet-3_0-mrel-spec.pdf
	 * @param uri: must prefixed with a '?' or a string ended with a '?'
	 * @return
	 */
	private static Map<String, String[]> getQueryParams(String uri) {

		Map<String, List<String>> queryParamsList;
		Map<String, String[]> queryParamsArray;

		QueryStringDecoder decoder = new QueryStringDecoder(uri);
		queryParamsList = decoder.parameters();

		if(queryParamsList.size() > 0) {
			queryParamsArray = new HashMap<>();
			for(Entry<String, List<String>> entry : queryParamsList.entrySet()) {
				queryParamsArray.put(entry.getKey(), entry.getValue().toArray(new String[0]));
			}
			return queryParamsArray;
		} else {
			return null;
		}
	}
}
