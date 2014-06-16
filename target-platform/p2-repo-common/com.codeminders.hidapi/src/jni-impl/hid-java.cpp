#include <stdlib.h>
#include <assert.h>
#include <jni.h>

#ifdef _WIN32
#include <windows.h>
#else
#include <iconv.h>
#endif

#include "hidapi/hidapi.h"
#include "hid-java.h"

void throwIOException(JNIEnv *env, hid_device *device)
{
    jclass exceptionClass;
    char *message = NULL;
    
    exceptionClass = env->FindClass("java/io/IOException");
    if (exceptionClass == NULL) 
    {
        /* Unable to find the exception class, give up. */
        assert(0);
        return;
    }
    
    if(device)
    {
        const wchar_t *error = hid_error(device);
        if(error) 
            message = convertToUTF8(env, error);
    }
    
    env->ThrowNew(exceptionClass, message ? message : ""); 
    
    free(message);
}

char* convertToUTF8(JNIEnv *env, const wchar_t *str)
{
#ifdef _WIN32
    int sz = WideCharToMultiByte(CP_UTF8, 0, str, -1, NULL, 0, NULL, NULL);    
    char *ret = (char *) malloc(sz + 1); 
    WideCharToMultiByte(CP_UTF8, 0, str, -1, ret, sz, NULL, NULL);    
    return ret;
#else
    iconv_t cd = iconv_open ("UTF-8", "WCHAR_T");
    if (cd == (iconv_t) -1)
    {
        /* Something went wrong. We could not recover from this  */
        
        jclass exceptionClass = env->FindClass("java/lang/Error");
        if (exceptionClass == NULL) 
        {
            /* Unable to find the exception class, give up. */
            assert(0);
            return NULL;
        }
    
        env->ThrowNew(exceptionClass, "iconv_open failed"); 
        return NULL;
    }
    size_t len = wcslen(str);
    size_t ulen = len*sizeof(wchar_t);
    char *uval = (char *)str;
    
    size_t u8l = len*6+3; //BOM+chars
    char *u8 = (char *) malloc(u8l+1);
    char *u8p = u8;
    int nconv = iconv(cd, &uval, &ulen, &u8p, &u8l);
    if(nconv == (size_t)-1)
    {
        iconv_close(cd);
        free(u8);
        jclass exceptionClass = env->FindClass("java/lang/Error");
        if (exceptionClass == NULL) 
        {
            /* Unable to find the exception class, give up. */
            assert(0);
            return NULL;
        }
        env->ThrowNew(exceptionClass, "iconv failed"); 
        return NULL;
    }
    *u8p='\0';

    iconv_close(cd);
    return u8;
#endif
}
