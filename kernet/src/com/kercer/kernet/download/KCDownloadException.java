package com.kercer.kernet.download;

/**
 * Created by zihong on 16/4/11.
 */


class KCUnexpectedStatusCodeException extends RuntimeException
{
    private static final long serialVersionUID = -6537360708511199076L;
}

class KCReachMaxRetryException extends RuntimeException
{
    private static final long serialVersionUID = 8493035725274498348L;

    public KCReachMaxRetryException(Throwable e)
    {
        super(e);
    }
}

class KCWrongStatusCodeException extends RuntimeException
{
    private static final long serialVersionUID = 1993527299811166227L;
}

 class KCZeroContentLengthException extends RuntimeException
{
    private static final long serialVersionUID = 178268877309938933L;
}
