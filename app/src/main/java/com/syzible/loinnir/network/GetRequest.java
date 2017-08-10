package com.syzible.loinnir.network;

/**
 * Created by ed on 16/12/2016
 */

abstract class GetRequest<T> extends Request<T> {
    GetRequest(NetworkCallback<T> networkCallback, String url, boolean isExternal) {
        super(networkCallback, url, "GET", isExternal);
    }
}